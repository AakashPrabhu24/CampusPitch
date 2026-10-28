package com.childchiller.campuspitch.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.childchiller.campuspitch.BuildConfig
import com.childchiller.campuspitch.core.constants.AppConstants

/**
 * Provider for Flash-class Gemini model supporting structured JSON schema
 * and zero-leak client initialization.
 */
object GeminiClientProvider {

    // Default to gemini-2.5-flash with fallback to gemini-1.5-flash
    const val PRIMARY_MODEL = AppConstants.GEMINI_MODEL_ID
    const val AUDIO_MODEL = AppConstants.GEMINI_AUDIO_MODEL_ID
    const val FALLBACK_MODEL = AppConstants.GEMINI_MODEL_FALLBACK

    /**
     * Resolves the configured Gemini API key from BuildConfig.
     */
    fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY.trim()
    }

    /**
     * Checks whether a valid Gemini API key is configured.
     * Rejects empty keys and the disabled project key to prevent HTTP 403 errors.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && !key.startsWith("AIzaSyBq75L32Wg")
    }

    /**
     * Builds a Flash-class GenerativeModel configured for JSON structured output
     * and safe venture pitch processing. Returns null if API key is not configured.
     */
    fun createModel(apiKey: String = getApiKey()): GenerativeModel? {
        if (apiKey.isBlank() || apiKey.startsWith("AIzaSyBq75L32Wg")) {
            return null
        }

        val config = generationConfig {
            temperature = 0.1f // Low temperature for deterministic extraction
            topK = 16
            topP = 0.9f
            responseMimeType = "application/json"
        }

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
        )

        return GenerativeModel(
            modelName = PRIMARY_MODEL,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings
        )
    }

    /**
     * Builds the cheapest current Flash-class GenerativeModel (gemini-1.5-flash) configured for
     * multimodal audio input (WAV/AAC) and strict structured JSON output with temperature ~0.
     * Returns null safely if API key is not configured.
     */
    fun createAudioModel(
        apiKey: String = getApiKey(),
        systemInstruction: String? = null
    ): GenerativeModel? {
        if (apiKey.isBlank() || apiKey.startsWith("AIzaSyBq75L32Wg")) {
            return null
        }

        val config = generationConfig {
            temperature = 0.0f // Zero temperature for precise multilingual transcription & extraction
            responseMimeType = "application/json"
        }

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
        )

        val sysContent = if (!systemInstruction.isNullOrBlank()) {
            com.google.ai.client.generativeai.type.Content(
                role = "system",
                parts = listOf(com.google.ai.client.generativeai.type.TextPart(systemInstruction))
            )
        } else null

        return GenerativeModel(
            modelName = AUDIO_MODEL,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings,
            systemInstruction = sysContent
        )
    }
}
