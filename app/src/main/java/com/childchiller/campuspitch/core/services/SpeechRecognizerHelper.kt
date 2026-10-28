package com.childchiller.campuspitch.core.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.childchiller.campuspitch.core.ai.DetectedLanguageInfo
import com.childchiller.campuspitch.core.ai.GeminiClientProvider
import com.childchiller.campuspitch.core.ai.MultiLanguageAudioResult
import com.childchiller.campuspitch.core.ai.MultiLanguageVoiceAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Universal Multilingual Speech Helper with Live Speech Typing and Zero-Picker Auto-Detection.
 *
 * Uses Android's native SpeechRecognizer for real-time live typing (word-by-word via onPartialResults)
 * combined with the MultiLanguageVoiceAgent for multi-language identification, currency normalization,
 * and structured field extraction.
 *
 * Resilient fallback to 16 kHz Mono Audio Recording + Gemini/Deterministic extraction.
 */
class SpeechRecognizerHelper(
    private val context: Context,
    private val voiceAgent: MultiLanguageVoiceAgent = MultiLanguageVoiceAgent(
        generativeModel = try {
            GeminiClientProvider.createAudioModel(systemInstruction = MultiLanguageVoiceAgent.SYSTEM_INSTRUCTION)
        } catch (_: Exception) {
            null
        }
    )
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var processingJob: Job? = null

    private val audioRecorder = AudioRecorderHelper(context)
    private var speechRecognizer: SpeechRecognizer? = null

    // Real-time reactive state streams
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _transcriptOriginal = MutableStateFlow("")
    val transcriptOriginal: StateFlow<String> = _transcriptOriginal.asStateFlow()

    private val _transcriptEnglish = MutableStateFlow("")
    val transcriptEnglish: StateFlow<String> = _transcriptEnglish.asStateFlow()

    private val _detectedLanguage = MutableStateFlow(DetectedLanguageInfo())
    val detectedLanguage: StateFlow<DetectedLanguageInfo> = _detectedLanguage.asStateFlow()

    private val _lastResult = MutableStateFlow<MultiLanguageAudioResult?>(null)
    val lastResult: StateFlow<MultiLanguageAudioResult?> = _lastResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var activeCallback: ((MultiLanguageAudioResult) -> Unit)? = null
    private var activeIntentHint: String? = null

    init {
        scope.launch {
            audioRecorder.error.collect { err ->
                if (err != null && _isListening.value) {
                    _error.value = err
                }
            }
        }
        scope.launch {
            audioRecorder.isRecording.collect { recording ->
                if (recording) _isListening.value = true
            }
        }
        scope.launch {
            audioRecorder.rmsDb.collect { level ->
                if (_isListening.value && speechRecognizer == null) {
                    _rmsDb.value = level
                }
            }
        }
    }

    /**
     * Starts listening with real-time speech typing and auto-detection.
     */
    fun startListening(
        forcedIntentHint: String? = null,
        onResult: ((MultiLanguageAudioResult) -> Unit)? = null
    ) {
        _error.value = null
        _partialTranscript.value = ""
        activeIntentHint = forcedIntentHint
        activeCallback = onResult

        mainHandler.post {
            val isSttAvailable = try {
                SpeechRecognizer.isRecognitionAvailable(context)
            } catch (_: Exception) {
                false
            }

            if (isSttAvailable) {
                startNativeSpeechRecognizer(forcedIntentHint)
            } else {
                startAudioRecordingFallback()
            }
        }
    }

    private fun startNativeSpeechRecognizer(forcedIntentHint: String?) {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _error.value = null
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsDb.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsDb.value = 0f
                    }

                    override fun onError(errorCode: Int) {
                        _isListening.value = false
                        _rmsDb.value = 0f

                        // If user already had recognized words, preserve and process them
                        val currentText = _transcript.value.ifBlank { _partialTranscript.value }
                        if (currentText.isNotBlank()) {
                            processTypedText(currentText, activeIntentHint, activeCallback)
                            return
                        }

                        // Try audio recorder fallback if recognizer was busy or audio issue
                        if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            errorCode == SpeechRecognizer.ERROR_AUDIO
                        ) {
                            startAudioRecordingFallback()
                            return
                        }

                        _error.value = when (errorCode) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly or type below."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please speak closer to mic or type."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue. Please check connection or type below."
                            else -> "Speech recognition error ($errorCode). You can also type below."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            if (recognized.isNotBlank()) {
                                _partialTranscript.value = recognized
                                _transcript.value = recognized
                                _detectedLanguage.value = DetectedLanguageInfo("en-IN", "English", 1.0)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsDb.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            _transcript.value = recognized
                            _partialTranscript.value = recognized
                            _detectedLanguage.value = DetectedLanguageInfo("en-IN", "English", 1.0)

                            // Process complete structured extraction
                            processTypedText(recognized, activeIntentHint, activeCallback)
                        } else {
                            if (_transcript.value.isBlank()) {
                                _error.value = "No speech detected. Please speak closer to mic or type below."
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            _isListening.value = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            startAudioRecordingFallback()
        }
    }

    private fun startAudioRecordingFallback() {
        _isListening.value = true
        audioRecorder.startRecording { wavBytes ->
            handleRecordedAudio(wavBytes)
        }
    }

    /**
     * Stops listening immediately and triggers structured processing.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}

            if (audioRecorder.isRecording.value) {
                val wavBytes = audioRecorder.stopRecording()
                if (wavBytes.isNotEmpty()) {
                    handleRecordedAudio(wavBytes)
                }
            }
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }

    private fun handleRecordedAudio(wavBytes: ByteArray) {
        if (wavBytes.isEmpty()) {
            _error.value = "No speech detected. Please speak closer to the mic or type below."
            return
        }

        _isProcessing.value = true
        _partialTranscript.value = "Detecting language & transcribing..."

        processingJob?.cancel()
        processingJob = scope.launch(Dispatchers.IO) {
            try {
                val result = voiceAgent.processAudio(wavBytes, activeIntentHint)

                scope.launch(Dispatchers.Main) {
                    _isProcessing.value = false
                    _lastResult.value = result

                    if (result.errorMessage != null && result.isFallback && result.transcriptOriginal.isBlank()) {
                        _error.value = result.errorMessage
                    } else {
                        _detectedLanguage.value = result.detectedLanguage
                        _transcriptOriginal.value = result.transcriptOriginal
                        _transcriptEnglish.value = result.transcriptEnglish
                        val primaryText = if (result.transcriptEnglish.isNotBlank()) result.transcriptEnglish else result.transcriptOriginal
                        _transcript.value = primaryText
                        _partialTranscript.value = primaryText

                        activeCallback?.invoke(result)
                    }
                }
            } catch (e: Exception) {
                scope.launch(Dispatchers.Main) {
                    _isProcessing.value = false
                    _error.value = "Voice processing error: ${e.localizedMessage ?: "Please try again or type."}"
                }
            }
        }
    }

    /**
     * Sets manual text (for typed input support).
     */
    fun setManualText(text: String) {
        _transcript.value = text
        _partialTranscript.value = text
        _transcriptOriginal.value = text
        _transcriptEnglish.value = text
        _error.value = null
        val lang = voiceAgent.detectLanguageLocally(text)
        _detectedLanguage.value = lang
    }

    /**
     * Processes typed text through the multi-language auto-detection agent.
     */
    fun processTypedText(
        text: String,
        forcedIntentHint: String? = null,
        onResult: ((MultiLanguageAudioResult) -> Unit)? = null
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _isProcessing.value = true
        _error.value = null
        setManualText(trimmed)

        processingJob?.cancel()
        processingJob = scope.launch(Dispatchers.IO) {
            val result = voiceAgent.processText(trimmed, forcedIntentHint)
            scope.launch(Dispatchers.Main) {
                _isProcessing.value = false
                _lastResult.value = result
                _detectedLanguage.value = result.detectedLanguage
                _transcriptOriginal.value = result.transcriptOriginal
                _transcriptEnglish.value = result.transcriptEnglish
                val primary = if (result.transcriptEnglish.isNotBlank()) result.transcriptEnglish else result.transcriptOriginal
                _transcript.value = primary
                _partialTranscript.value = primary
                onResult?.invoke(result)
            }
        }
    }

    /**
     * Allows user to manually override the detected language from the Confirmation Card chip.
     */
    fun overrideDetectedLanguage(override: DetectedLanguageInfo) {
        _detectedLanguage.value = override
        val current = _lastResult.value
        if (current != null) {
            _lastResult.value = current.copy(detectedLanguage = override)
        }
    }

    fun clear() {
        _transcript.value = ""
        _partialTranscript.value = ""
        _transcriptOriginal.value = ""
        _transcriptEnglish.value = ""
        _lastResult.value = null
        _error.value = null
        _rmsDb.value = 0f
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
        audioRecorder.cancelRecording()
        processingJob?.cancel()
    }
}
