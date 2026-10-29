package com.childchiller.campuspitch.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.models.MvpPitchRecord
import com.childchiller.campuspitch.core.utils.InrParser
import org.json.JSONObject
import java.util.UUID

/**
 * Agent Definition:
 * - Name: StudentIntakeAgent
 * - Job: Converts unconstrained natural sentences into structured MVP entities without forms,
 *        detects missing essentials, and formulates ONE short conversational follow-up.
 * - Inputs: Spoken or typed natural sentence, active draft context, student UID.
 * - Outputs: StudentIntakeResult (structured MvpPitchRecord, missing field, follow-up query, confidence scores).
 * - Failure Handling: Transparent fallback to deterministic rule engine (InrParser + Regex taxonomy).
 */
data class StudentIntakeResult(
    val draftMvp: MvpPitchRecord?,
    val missingEssentialField: String? = null,
    val followUpQuestion: String? = null,
    val fieldConfidence: Map<String, Double> = emptyMap(),
    val isVoiceEdit: Boolean = false,
    val rawResponse: String = "",
    val executionLatencyMs: Long = 0L
)

class StudentIntakeAgent(
    private val generativeModel: GenerativeModel? = null
) {
    val agentName = "StudentIntakeAgent"
    val modelVersion = AppConstants.GEMINI_MODEL_ID

    /**
     * Executes the intake extraction pipeline.
     */
    suspend fun processPitchSentence(
        inputSentence: String,
        studentUid: String,
        currentDraft: MvpPitchRecord? = null
    ): StudentIntakeResult {
        val startTime = System.currentTimeMillis()
        val cleanedInput = inputSentence.trim()

        if (cleanedInput.isBlank()) {
            return StudentIntakeResult(
                draftMvp = null,
                executionLatencyMs = System.currentTimeMillis() - startTime
            )
        }

        // 1. Check if the utterance is a Voice Edit of an existing draft (e.g. "change my raise to 2 cr")
        if (currentDraft != null && isVoiceEditUtterance(cleanedInput)) {
            val edited = applyVoiceEdit(cleanedInput, currentDraft)
            return StudentIntakeResult(
                draftMvp = edited,
                isVoiceEdit = true,
                fieldConfidence = mapOf("raiseAmountInr" to 0.99, "edited" to 1.0),
                executionLatencyMs = System.currentTimeMillis() - startTime
            )
        }

        // 2. Try Gemini Flash structured JSON extraction if available
        if (generativeModel != null) {
            try {
                val prompt = buildExtractionPrompt(cleanedInput, currentDraft)
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                val parsed = parseGeminiJsonResponse(responseText, cleanedInput, studentUid, currentDraft)
                if (parsed != null) {
                    return parsed.copy(
                        executionLatencyMs = System.currentTimeMillis() - startTime,
                        rawResponse = responseText
                    )
                }
            } catch (e: Exception) {
                // Network/quota failure -> Fallback to deterministic NLP engine
            }
        }

        // 3. Fallback: High-precision deterministic NLP parser
        val fallbackResult = executeDeterministicExtraction(cleanedInput, studentUid, currentDraft)
        return fallbackResult.copy(
            executionLatencyMs = System.currentTimeMillis() - startTime
        )
    }

    private fun isVoiceEditUtterance(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("change ") ||
                lower.startsWith("update ") ||
                lower.startsWith("make it ") ||
                lower.contains("change my raise") ||
                lower.contains("update raise to")
    }

    private fun applyVoiceEdit(text: String, existing: MvpPitchRecord): MvpPitchRecord {
        val lower = text.lowercase()
        val inrResult = InrParser.parse(text)

        val updatedAmount = inrResult.singleAmountInr ?: existing.raiseAmountInr
        val updatedWording = if (inrResult.singleAmountInr != null) inrResult.originalWording else existing.raiseOriginalWording

        // Check if description is being edited
        val updatedDesc = if (lower.contains("description to")) {
            text.substringAfterIgnoreCase("description to").trim()
        } else {
            existing.description
        }

        return existing.copy(
            raiseAmountInr = updatedAmount,
            raiseOriginalWording = updatedWording,
            description = updatedDesc,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun buildExtractionPrompt(input: String, existingDraft: MvpPitchRecord?): String {
        return """
            You are the StudentIntakeAgent for CampusPitch. Extract startup pitch data from student input into strict JSON.
            
            Taxonomy Sectors:
            ${AppConstants.CANONICAL_SECTORS.joinToString("\n") { "- $it" }}
            
            Indian Currency Rules:
            - 1 crore (cr) = 10,000,000 INR
            - 1 lakh = 100,000 INR
            - "one and a half crore" / "dedh crore" = 15,000,000 INR
            
            Rules:
            - DO NOT hallucinate. Unknown fields must be null.
            - If app name or description or raise amount is completely missing, flag it in missingField and provide ONE polite, short conversational followUpQuestion.
            - Confidence score must be between 0.0 and 1.0 per field.
            
            JSON Format:
            {
              "appName": "string or null",
              "description": "string or null",
              "sector": "string from taxonomy or null",
              "stage": "Idea | Prototype | MVP | Early Revenue",
              "raiseAmountInr": 10000000,
              "raiseOriginalWording": "1 cr",
              "links": ["https://..."],
              "missingField": "raiseAmountInr | appName | description | null",
              "followUpQuestion": "Short follow up question if missingField is present, else null",
              "confidence": {
                "appName": 0.95,
                "description": 0.90,
                "sector": 0.92,
                "raiseAmountInr": 0.98
              }
            }
            
            Student sentence: "$input"
        """.trimIndent()
    }

    private fun parseGeminiJsonResponse(
        jsonString: String,
        originalInput: String,
        studentUid: String,
        currentDraft: MvpPitchRecord?
    ): StudentIntakeResult? {
        return try {
            val cleanJson = jsonString.trim().removeSurrounding("```json", "```").trim()
            val root = JSONObject(cleanJson)

            val appName = root.optString("appName").takeIf { it.isNotBlank() && it != "null" }
                ?: currentDraft?.appName
            val description = root.optString("description").takeIf { it.isNotBlank() && it != "null" }
                ?: currentDraft?.description
            val sector = root.optString("sector").takeIf { it.isNotBlank() && it != "null" }
                ?: matchSector(originalInput)
            val stage = root.optString("stage", "MVP")
            val raiseInr = root.optLong("raiseAmountInr", 0L).takeIf { it > 0 }
                ?: InrParser.parse(originalInput).singleAmountInr ?: currentDraft?.raiseAmountInr ?: 0L
            val raiseWording = root.optString("raiseOriginalWording").takeIf { it.isNotBlank() }
                ?: InrParser.parse(originalInput).originalWording.ifBlank { "N/A" }

            val missingField = root.optString("missingField").takeIf { it.isNotBlank() && it != "null" }
            val followUp = root.optString("followUpQuestion").takeIf { it.isNotBlank() && it != "null" }

            val confidenceMap = mutableMapOf<String, Double>()
            root.optJSONObject("confidence")?.let { confJson ->
                val keys = confJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    confidenceMap[k] = confJson.optDouble(k, 0.90)
                }
            }

            val draft = if (appName != null && description != null) {
                MvpPitchRecord(
                    id = currentDraft?.id ?: UUID.randomUUID().toString(),
                    studentUid = studentUid,
                    appName = appName,
                    description = description,
                    sector = sector,
                    stage = stage,
                    raiseAmountInr = raiseInr,
                    raiseOriginalWording = raiseWording,
                    fieldConfidence = confidenceMap,
                    rawInputId = UUID.randomUUID().toString(),
                    updatedAt = System.currentTimeMillis()
                )
            } else null

            StudentIntakeResult(
                draftMvp = draft,
                missingEssentialField = missingField,
                followUpQuestion = followUp,
                fieldConfidence = confidenceMap
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deterministic Rule-Based Extraction fallback.
     */
    fun executeDeterministicExtraction(
        input: String,
        studentUid: String,
        currentDraft: MvpPitchRecord? = null
    ): StudentIntakeResult {
        val lower = input.lowercase()

        // 1. Extract App Name
        val appName = when {
            lower.contains("called") -> {
                input.substringAfterIgnoreCase("called").split(" ", ".", ",").firstOrNull { it.isNotBlank() }?.replace(Regex("[^a-zA-Z0-9]"), "")
            }
            lower.contains("named") -> {
                input.substringAfterIgnoreCase("named").split(" ", ".", ",").firstOrNull { it.isNotBlank() }?.replace(Regex("[^a-zA-Z0-9]"), "")
            }
            lower.contains("app is") -> {
                input.substringAfterIgnoreCase("app is").split(" ", ".", ",").firstOrNull { it.isNotBlank() }?.replace(Regex("[^a-zA-Z0-9]"), "")
            }
            currentDraft != null -> currentDraft.appName
            else -> null
        }

        // 2. Extract Raise Amount using INR Parser
        val inrResult = InrParser.parse(input)
        val raiseInr = inrResult.singleAmountInr ?: currentDraft?.raiseAmountInr ?: 0L
        val raiseWording = if (inrResult.singleAmountInr != null) inrResult.originalWording else currentDraft?.raiseOriginalWording ?: ""

        // 3. Extract Sector
        val sector = matchSector(input)

        // 4. Extract Description
        val description = when {
            lower.contains("it does") -> {
                input.substringAfterIgnoreCase("it does")
                    .substringBeforeIgnoreCase("I am looking")
                    .substringBeforeIgnoreCase("looking to")
                    .substringBeforeIgnoreCase("seeking")
                    .substringBeforeIgnoreCase("and I want")
                    .trim().trimEnd('.', ',')
            }
            lower.contains("does") -> {
                input.substringAfterIgnoreCase("does")
                    .substringBeforeIgnoreCase("I am looking")
                    .substringBeforeIgnoreCase("looking to")
                    .substringBeforeIgnoreCase("seeking")
                    .substringBeforeIgnoreCase("and I want")
                    .trim().trimEnd('.', ',')
            }
            lower.contains("called") && input.contains(".") -> {
                val afterApp = input.substringAfter(".")
                afterApp.substringBeforeIgnoreCase("and I want")
                    .substringBeforeIgnoreCase("I want to raise")
                    .substringBeforeIgnoreCase("I am looking")
                    .substringBeforeIgnoreCase("looking to")
                    .substringBeforeIgnoreCase("seeking")
                    .trim().trimEnd('.', ',')
            }
            currentDraft != null -> currentDraft.description
            else -> input.trim()
        }

        // 5. Detect Missing Essential Details & Ask ONE Follow-Up
        var missingField: String? = null
        var followUpQuestion: String? = null

        when {
            appName.isNullOrBlank() -> {
                missingField = "appName"
                followUpQuestion = "What is the name of your app or MVP?"
            }
            raiseInr == 0L -> {
                missingField = "raiseAmountInr"
                followUpQuestion = "How much capital are you looking to raise to scale ${appName ?: "this app"}?"
            }
            description.isBlank() || description.length < 5 -> {
                missingField = "description"
                followUpQuestion = "Can you share in one sentence what your app does?"
            }
        }

        val confidence = mapOf(
            "appName" to if (appName != null) 0.98 else 0.0,
            "raiseAmountInr" to if (raiseInr > 0L) 0.99 else 0.0,
            "sector" to 0.95,
            "description" to if (description.isNotBlank()) 0.92 else 0.0
        )

        val draft = if (appName != null && description.isNotBlank()) {
            MvpPitchRecord(
                id = currentDraft?.id ?: UUID.randomUUID().toString(),
                studentUid = studentUid,
                appName = appName,
                description = description,
                sector = sector,
                stage = "MVP",
                raiseAmountInr = raiseInr,
                raiseOriginalWording = raiseWording.ifBlank { "N/A" },
                oneLineAiSummary = "Campus MVP in $sector: $description",
                fieldConfidence = confidence,
                rawInputId = UUID.randomUUID().toString(),
                updatedAt = System.currentTimeMillis()
            )
        } else null

        return StudentIntakeResult(
            draftMvp = draft,
            missingEssentialField = missingField,
            followUpQuestion = followUpQuestion,
            fieldConfidence = confidence
        )
    }

    private fun matchSector(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("hospital") || lower.contains("clinic") || lower.contains("doctor") || lower.contains("health") || lower.contains("med") ->
                "HealthTech & Hospital Management"
            lower.contains("pay") || lower.contains("money") || lower.contains("wallet") || lower.contains("upi") || lower.contains("finance") || lower.contains("bank") ->
                "FinTech & Payments"
            lower.contains("campus") || lower.contains("college") || lower.contains("student") || lower.contains("study") || lower.contains("exam") || lower.contains("edtech") ->
                "EdTech & Campus Tools"
            lower.contains("farm") || lower.contains("agri") || lower.contains("crop") || lower.contains("farmer") ->
                "AgriTech & Supply Chain"
            lower.contains("climate") || lower.contains("ev") || lower.contains("battery") || lower.contains("solar") || lower.contains("green") ->
                "ClimateTech & EV"
            lower.contains("store") || lower.contains("shop") || lower.contains("food") || lower.contains("order") || lower.contains("d2c") ->
                "Consumer & D2C"
            lower.contains("hardware") || lower.contains("iot") || lower.contains("robot") || lower.contains("chip") || lower.contains("drone") ->
                "DeepTech & Hardware"
            else ->
                "AI, SaaS & DevTools"
        }
    }
}
private fun String.substringAfterIgnoreCase(delimiter: String): String {
    val idx = this.indexOf(delimiter, ignoreCase = true)
    return if (idx >= 0) this.substring(idx + delimiter.length) else this
}

private fun String.substringBeforeIgnoreCase(delimiter: String): String {
    val idx = this.indexOf(delimiter, ignoreCase = true)
    return if (idx >= 0) this.substring(0, idx) else this
}

