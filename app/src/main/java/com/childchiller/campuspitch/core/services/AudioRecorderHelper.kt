package com.childchiller.campuspitch.core.services

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * 16 kHz Mono Audio Recorder for Multimodal AI Audio Ingestion.
 *
 * Specifications:
 * - Sampling Rate: 16,000 Hz (16 kHz mono)
 * - Channel: AudioFormat.CHANNEL_IN_MONO
 * - Bit Depth: 16-bit Linear PCM (AudioFormat.ENCODING_PCM_16BIT)
 * - Waveform Visualizer: Emits real-time normalized RMS amplitude (0.0f .. 1.0f)
 * - Max Duration: 60 seconds (auto-stops)
 * - Silence Detection: Auto-stops after 2.5 seconds of sustained silence once speech is detected
 * - Ephemeral Storage: Audio is processed in-memory as a WAV byte array; any temporary files are deleted immediately
 */
class AudioRecorderHelper(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
        const val MAX_RECORDING_DURATION_MS = 60_000L
        const val SILENCE_THRESHOLD_MS = 2500L
        const val SPEECH_RMS_THRESHOLD = 0.06f
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val pcmOutputStream = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    fun startRecording(onAutoStop: ((ByteArray) -> Unit)? = null) {
        if (_isRecording.value) return

        pcmOutputStream.reset()
        _error.value = null
        _rmsDb.value = 0f

        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            2048
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _error.value = "Microphone failed to initialize. Please check permissions."
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch {
                val buffer = ShortArray(1024)
                val startTime = System.currentTimeMillis()
                var speechDetected = false
                var lastSpeechTime = startTime

                while (isActive && _isRecording.value) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        // Write raw PCM bytes (little-endian)
                        val byteBuffer = ByteBuffer.allocate(readCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                        var sumOfSquares = 0.0
                        for (i in 0 until readCount) {
                            val sample = buffer[i]
                            byteBuffer.putShort(sample)
                            sumOfSquares += sample * sample
                        }
                        synchronized(pcmOutputStream) {
                            pcmOutputStream.write(byteBuffer.array())
                        }

                        // Calculate normalized RMS amplitude for animated neon visualizer
                        val rms = Math.sqrt(sumOfSquares / readCount)
                        val normalized = ((rms / 3000.0).coerceIn(0.0, 1.0)).toFloat()
                        _rmsDb.value = normalized

                        val now = System.currentTimeMillis()

                        // Detect speech vs silence
                        if (normalized >= SPEECH_RMS_THRESHOLD) {
                            speechDetected = true
                            lastSpeechTime = now
                        }

                        // Auto-stop on 2.5s silence after speech or max 60s
                        val duration = now - startTime
                        if (duration >= MAX_RECORDING_DURATION_MS ||
                            (speechDetected && (now - lastSpeechTime) >= SILENCE_THRESHOLD_MS)
                        ) {
                            val wavBytes = stopInternal()
                            if (wavBytes.isNotEmpty() && onAutoStop != null) {
                                onAutoStop(wavBytes)
                            }
                            break
                        }
                    } else if (readCount < 0) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            _error.value = "Recording error: ${e.localizedMessage ?: "Unknown"}"
            stopRecording()
        }
    }

    fun stopRecording(): ByteArray {
        return stopInternal()
    }

    private fun stopInternal(): ByteArray {
        _isRecording.value = false
        _rmsDb.value = 0f
        recordingJob?.cancel()
        recordingJob = null

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        val pcmData: ByteArray
        synchronized(pcmOutputStream) {
            pcmData = pcmOutputStream.toByteArray()
            pcmOutputStream.reset()
        }

        if (pcmData.isEmpty()) {
            return ByteArray(0)
        }

        return createWavBytes(pcmData, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE)
    }

    fun cancelRecording() {
        _isRecording.value = false
        _rmsDb.value = 0f
        recordingJob?.cancel()
        recordingJob = null

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        synchronized(pcmOutputStream) {
            pcmOutputStream.reset()
        }
    }

    /**
     * Constructs a standard 44-byte RIFF/WAVE header for linear PCM audio data.
     */
    fun createWavBytes(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1.toShort()) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        // data chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(totalAudioLen)

        val out = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, out, 0, 44)
        System.arraycopy(pcmData, 0, out, 44, totalAudioLen)
        return out
    }

    /**
     * Helper to write temporary WAV file and safely execute a block with it,
     * ensuring immediate deletion after execution (Requirement: "Do not store audio; delete temp files after processing").
     */
    fun withTempWavFile(wavBytes: ByteArray, block: (File) -> Unit) {
        val tempFile = File(context.cacheDir, "voice_intake_${UUID.randomUUID()}.wav")
        try {
            FileOutputStream(tempFile).use { it.write(wavBytes) }
            block(tempFile)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
