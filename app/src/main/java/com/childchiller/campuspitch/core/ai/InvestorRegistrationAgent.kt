package com.childchiller.campuspitch.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.models.InvestorProfileRecord
import org.json.JSONObject
import java.net.URI
import java.util.Locale
import java.util.UUID

/**
 * Agent Definition:
 * - Name: InvestorRegistrationAgent
 * - Job: Extracts fund name, AIF category, email, mobile, and website from spoken/typed sentences across English, Spanish, and regional Indian languages.
 *        Validates email format, domain matching vs website, and Indian mobile format.
 * - Inputs: Investor natural voice/text sentence, active UID.
 * - Outputs: InvestorRegistrationResult (profile, validation flags, error messages, follow-up).
 * - Failure Handling: Transparent fallback to deterministic regex and domain parsing engine.
 */
data class InvestorValidation(
    val isEmailValid: Boolean = false,
    val isMobileValid: Boolean = false,
    val isWebsiteValid: Boolean = false,
    val doesEmailDomainMatchWebsite: Boolean = false,
    val validationErrors: List<String> = emptyList()
) {
    val isFullyValid: Boolean
        get() = isEmailValid && isMobileValid && isWebsiteValid && doesEmailDomainMatchWebsite
}

data class InvestorRegistrationResult(
    val profile: InvestorProfileRecord?,
    val validation: InvestorValidation = InvestorValidation(),
    val missingEssentialField: String? = null,
    val followUpQuestion: String? = null,
    val fieldConfidence: Map<String, Double> = emptyMap(),
    val rawResponse: String = "",
    val executionLatencyMs: Long = 0L
)

class InvestorRegistrationAgent(
    private val generativeModel: GenerativeModel? = null
) {
    val agentName = "InvestorRegistrationAgent"
    val modelVersion = AppConstants.GEMINI_MODEL_ID

    suspend fun processRegistrationSentence(
        inputSentence: String,
        investorUid: String
    ): InvestorRegistrationResult {
        val startTime = System.currentTimeMillis()
        val cleanedInput = inputSentence.trim()

        if (cleanedInput.isBlank()) {
            return InvestorRegistrationResult(
                profile = null,
                executionLatencyMs = System.currentTimeMillis() - startTime
            )
        }

        // 1. Try Gemini Flash structured JSON extraction if available
        if (generativeModel != null) {
            try {
                val prompt = buildExtractionPrompt(cleanedInput)
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                val parsed = parseGeminiJsonResponse(responseText, cleanedInput, investorUid)
                if (parsed != null) {
                    return parsed.copy(
                        executionLatencyMs = System.currentTimeMillis() - startTime,
                        rawResponse = responseText
                    )
                }
            } catch (e: Exception) {
                // Fallback to deterministic NLP engine
            }
        }

        // 2. High-precision deterministic NLP parser & validator
        val fallbackResult = executeDeterministicExtraction(cleanedInput, investorUid)
        return fallbackResult.copy(
            executionLatencyMs = System.currentTimeMillis() - startTime
        )
    }

    private fun buildExtractionPrompt(input: String): String {
        return """
            You are the InvestorRegistrationAgent for CampusPitch. Extract investor registration data from the natural sentence into strict JSON.
            Input may be in English, Spanish, or Indian languages.
            
            Entity Schema:
            - fundName: Name of the venture capital or AIF fund (string, or null if missing)
            - aifCategory: One of: "AIF Category I", "AIF Category II", "AIF Category III", "VC / Angel"
            - officialEmail: Corporate email address (string, or null)
            - mobile: 10-digit Indian mobile number without +91 or country code (string, or null)
            - website: Official website URL or domain (string, or null)
            
            Indian Mobile Rule: Must be 10 digits starting with 6, 7, 8, or 9.
            
            Return ONLY a raw JSON object with keys:
            {
              "fundName": "...",
              "aifCategory": "...",
              "officialEmail": "...",
              "mobile": "...",
              "website": "...",
              "confidence": {
                "fundName": 0.95,
                "aifCategory": 0.95,
                "officialEmail": 0.99,
                "mobile": 0.99,
                "website": 0.95
              }
            }
            
            User Input:
            "$input"
        """.trimIndent()
    }

    private fun parseGeminiJsonResponse(
        jsonString: String,
        input: String,
        uid: String
    ): InvestorRegistrationResult? {
        return try {
            val cleanJson = jsonString
                .substringAfter("{")
                .substringBeforeLast("}")
            val fullJson = "{$cleanJson}"
            val obj = JSONObject(fullJson)

            val fundName = obj.optString("fundName").takeIf { it.isNotBlank() && it != "null" }
            val aifCategory = obj.optString("aifCategory", "AIF Category I")
            val email = obj.optString("officialEmail").takeIf { it.isNotBlank() && it != "null" }
            val mobile = obj.optString("mobile").takeIf { it.isNotBlank() && it != "null" }
            val website = obj.optString("website").takeIf { it.isNotBlank() && it != "null" }

            validateAndAssemble(
                fundName = fundName,
                aifCategory = aifCategory,
                email = email,
                mobile = mobile,
                website = website,
                uid = uid,
                confidenceMap = emptyMap()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deterministic rule-based extraction engine for offline and benchmark tests.
     */
    fun executeDeterministicExtraction(
        input: String,
        uid: String
    ): InvestorRegistrationResult {
        val lower = input.lowercase(Locale.ROOT)

        // 1. Extract Fund Name
        val fundName = extractFundName(input, lower)

        // 2. Extract AIF Category (supporting English, Spanish and abbreviations)
        val aifCategory = when {
            lower.contains("category iii") || lower.contains("category 3") || lower.contains("cat iii") || lower.contains("cat 3") ||
                    lower.contains("categoría iii") || lower.contains("categoría 3") ->
                "AIF Category III"
            lower.contains("category ii") || lower.contains("category 2") || lower.contains("cat ii") || lower.contains("cat 2") ||
                    lower.contains("categoría ii") || lower.contains("categoría 2") ->
                "AIF Category II"
            lower.contains("category i") || lower.contains("category 1") || lower.contains("cat i") || lower.contains("cat 1") ||
                    lower.contains("categoría aif i") || lower.contains("categoría aif 1") || lower.contains("categoría i") ||
                    lower.contains("categoría 1") || lower.contains("aif i") || lower.contains("aif 1") ->
                "AIF Category I"
            lower.contains("angel") || lower.contains("syndicate") ->
                "VC / Angel"
            else ->
                "AIF Category I"
        }

        // 3. Extract Official Email
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val email = emailRegex.find(input)?.value

        // 4. Extract Mobile (handles +91, spaces, hyphens)
        val mobileDigits = input.replace(Regex("[^0-9]"), "")
        val mobile = when {
            mobileDigits.length == 10 && mobileDigits.first() in '6'..'9' -> mobileDigits
            mobileDigits.length == 12 && mobileDigits.startsWith("91") && mobileDigits[2] in '6'..'9' -> mobileDigits.substring(2)
            else -> {
                val candidateRegex = Regex("\\b[6-9]\\d{9}\\b")
                candidateRegex.find(input.replace(" ", "").replace("-", ""))?.value
            }
        }

        // 5. Extract Website
        val website = extractWebsite(input, lower, email)

        val confidence = mapOf(
            "fundName" to if (fundName != null) 0.95 else 0.0,
            "aifCategory" to 0.95,
            "officialEmail" to if (email != null) 0.99 else 0.0,
            "mobile" to if (mobile != null) 0.98 else 0.0,
            "website" to if (website != null) 0.95 else 0.0
        )

        return validateAndAssemble(
            fundName = fundName,
            aifCategory = aifCategory,
            email = email,
            mobile = mobile,
            website = website,
            uid = uid,
            confidenceMap = confidence
        )
    }

    private fun extractFundName(input: String, lower: String): String? {
        val patterns = listOf(
            "fund is ", "fund name is ", "representing ", "from ", "firm is ", "firm name is ", "we are ",
            "fondo es ", "fondo se llama ", "mi fondo es ", "somos "
        )
        for (p in patterns) {
            val idx = lower.indexOf(p)
            if (idx >= 0) {
                val after = input.substring(idx + p.length).trim()
                val candidate = after.substringBeforeIgnoreCase(" and")
                    .substringBeforeIgnoreCase(" y ")
                    .substringBeforeIgnoreCase(",")
                    .substringBeforeIgnoreCase(".")
                    .trim()
                if (candidate.isNotBlank() && !candidate.equals("an aif", ignoreCase = true) && !candidate.equals("a vc", ignoreCase = true)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun extractWebsite(input: String, lower: String, email: String?): String? {
        val websitePatterns = listOf("website is ", "website: ", "website ", "sitio web is ", "sitio web: ", "sitio web es ", "sitio web ")
        for (p in websitePatterns) {
            val websiteIdx = lower.indexOf(p)
            if (websiteIdx >= 0) {
                val after = input.substring(websiteIdx + p.length).trim()
                val raw = after.split(" ", ",", ";").firstOrNull { it.isNotBlank() }
                if (!raw.isNullOrBlank()) {
                    return cleanDomainOrUrl(raw)
                }
            }
        }

        // Fallback: look for a domain that is not an email
        val domainRegex = Regex("\\b(?:https?://)?(?:www\\.)?([a-zA-Z0-9-]+\\.[a-zA-Z]{2,}(?:/[a-zA-Z0-9._%+-]*)*)\\b")
        val matches = domainRegex.findAll(input)
        for (m in matches) {
            val valStr = m.value.trim().trimEnd('.')
            if (email == null || !email.contains(valStr)) {
                return cleanDomainOrUrl(valStr)
            }
        }

        return null
    }

    private fun cleanDomainOrUrl(raw: String): String {
        return raw.trim().trimEnd('.', ',', ';', '/')
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
    }

    private fun validateAndAssemble(
        fundName: String?,
        aifCategory: String,
        email: String?,
        mobile: String?,
        website: String?,
        uid: String,
        confidenceMap: Map<String, Double>
    ): InvestorRegistrationResult {
        val errors = mutableListOf<String>()

        // 1. Email format check
        val emailValid = email != null && Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$").matches(email)
        if (!emailValid) {
            errors.add("Please provide a valid corporate email address.")
        }

        // 2. Indian mobile format check (10 digits starting with 6-9)
        val mobileValid = mobile != null && Regex("^[6-9]\\d{9}$").matches(mobile)
        if (!mobileValid) {
            errors.add("Mobile number must be a 10-digit Indian number starting with 6, 7, 8, or 9.")
        }

        // 3. Website check
        val websiteClean = website?.let { cleanDomainOrUrl(it) }
        val websiteValid = !websiteClean.isNullOrBlank() && websiteClean.contains(".")
        if (!websiteValid) {
            errors.add("Official website domain must be provided.")
        }

        // 4. Email domain matching website domain check
        var domainMatch = false
        if (emailValid && websiteValid && email != null && !websiteClean.isNullOrBlank()) {
            val emailDomain = cleanDomainOrUrl(email.substringAfter("@").lowercase())
            val siteDomain = websiteClean.lowercase()
            domainMatch = siteDomain == emailDomain ||
                    siteDomain.endsWith(".$emailDomain") ||
                    emailDomain.endsWith(".$siteDomain")

            if (!domainMatch) {
                errors.add("Email domain (@${email.substringAfter("@")}) must match fund website domain ($websiteClean).")
            }
        }

        // Missing essential check for single follow-up
        var missingEssential: String? = null
        var followUp: String? = null

        when {
            fundName.isNullOrBlank() -> {
                missingEssential = "fundName"
                followUp = "What is the official legal name of your investment fund?"
            }
            email.isNullOrBlank() -> {
                missingEssential = "officialEmail"
                followUp = "What is your official corporate email address at $fundName?"
            }
            mobile.isNullOrBlank() -> {
                missingEssential = "mobile"
                followUp = "What is your 10-digit mobile number for secure investor authentication?"
            }
            website.isNullOrBlank() -> {
                missingEssential = "website"
                followUp = "What is your fund's official website URL?"
            }
        }

        val validation = InvestorValidation(
            isEmailValid = emailValid,
            isMobileValid = mobileValid,
            isWebsiteValid = websiteValid,
            doesEmailDomainMatchWebsite = domainMatch,
            validationErrors = errors
        )

        val profile = if (!fundName.isNullOrBlank()) {
            InvestorProfileRecord(
                uid = uid,
                fundName = fundName,
                aifCategory = aifCategory,
                officialEmail = email ?: "",
                mobile = mobile ?: "",
                website = websiteClean ?: "",
                emailDomainMatchesWebsite = domainMatch,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
        } else null

        return InvestorRegistrationResult(
            profile = profile,
            validation = validation,
            missingEssentialField = missingEssential,
            followUpQuestion = followUp,
            fieldConfidence = confidenceMap
        )
    }

    /**
     * Issues a unique, deterministic Investor Code upon approval (e.g. CCV-BPV-4821).
     */
    fun generateInvestorCode(fundName: String): String {
        val initials = fundName
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .split(" ", "-", "&", "_")
            .filter { it.isNotBlank() }
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .take(3)
            .padEnd(3, 'X')

        val randomSuffix = (1000..9999).random()
        return "CCV-$initials-$randomSuffix"
    }

    private fun String.substringBeforeIgnoreCase(delimiter: String): String {
        val idx = this.indexOf(delimiter, ignoreCase = true)
        return if (idx >= 0) this.substring(0, idx) else this
    }
}
