package com.childchiller.campuspitch.core.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.models.MvpPitchRecord
import com.childchiller.campuspitch.core.utils.InrParser
import org.json.JSONObject
import java.util.Locale

/**
 * Agent Definition:
 * - Name: InvestorSearchAgent
 * - Job: Converts unconstrained natural investor search queries into a structured query
 *        (target sector with synonyms, min/max INR range, keywords), filters and ranks
 *        available MVPs from the Data Lake, generates one-line AI summaries, and drafts
 *        contextual introductory outreach messages.
 * - Inputs: Investor search utterance (spoken/typed), candidate MVP list from Data Lake.
 * - Outputs: InvestorSearchResult (structured query, ranked MVPs with confidence & summary, drafted intro message).
 * - Failure Handling: Transparent fallback to deterministic INR parser and sector taxonomy matching.
 */

data class StructuredSearchQuery(
    val originalUtterance: String = "",
    val targetSector: String? = null,
    val sectorSynonyms: List<String> = emptyList(),
    val minRaiseInr: Long? = null,
    val maxRaiseInr: Long? = null,
    val keywords: List<String> = emptyList(),
    val stagePreference: String? = null
) {
    val formattedRange: String
        get() = when {
            minRaiseInr != null && maxRaiseInr != null ->
                "₹${InrParser.formatInr(minRaiseInr)} - ₹${InrParser.formatInr(maxRaiseInr)}"
            minRaiseInr != null -> "Min ₹${InrParser.formatInr(minRaiseInr)}"
            maxRaiseInr != null -> "Up to ₹${InrParser.formatInr(maxRaiseInr)}"
            else -> "Any Amount"
        }
}

data class RankedMvpResult(
    val mvp: MvpPitchRecord,
    val relevanceScore: Double, // 0.0 to 1.0
    val oneLineAiSummary: String,
    val matchReasons: List<String> = emptyList(),
    val draftedIntroMessage: String = ""
)

data class InvestorSearchResult(
    val query: StructuredSearchQuery,
    val rankedResults: List<RankedMvpResult>,
    val executionLatencyMs: Long = 0L,
    val rawResponse: String = ""
)

class InvestorSearchAgent(
    private val generativeModel: GenerativeModel? = null
) {
    val agentName = "InvestorSearchAgent"
    val modelVersion = AppConstants.GEMINI_MODEL_ID

    suspend fun executeSearch(
        utterance: String,
        candidateMvps: List<MvpPitchRecord>,
        investorFundName: String = "Venture Partner"
    ): InvestorSearchResult {
        val startTime = System.currentTimeMillis()
        val cleanedUtterance = utterance.trim()

        if (cleanedUtterance.isBlank()) {
            return InvestorSearchResult(
                query = StructuredSearchQuery(),
                rankedResults = candidateMvps.map { mvp ->
                    RankedMvpResult(
                        mvp = mvp,
                        relevanceScore = 0.5,
                        oneLineAiSummary = mvp.oneLineAiSummary ?: "${mvp.appName}: ${mvp.description}",
                        draftedIntroMessage = draftIntroMessage(mvp, investorFundName)
                    )
                },
                executionLatencyMs = System.currentTimeMillis() - startTime
            )
        }

        // 1. Try Gemini Flash structured JSON extraction for search query
        var structuredQuery: StructuredSearchQuery? = null
        var rawApiResponse = ""

        if (generativeModel != null) {
            try {
                val prompt = buildSearchPrompt(cleanedUtterance)
                val response = generativeModel.generateContent(prompt)
                rawApiResponse = response.text ?: ""
                structuredQuery = parseGeminiSearchJson(rawApiResponse, cleanedUtterance)
            } catch (e: Exception) {
                // Fall back to deterministic NLP
            }
        }

        if (structuredQuery == null) {
            structuredQuery = executeDeterministicQueryParsing(cleanedUtterance)
        }

        // 2. Rank candidate MVPs against the structured query
        val ranked = rankMvps(structuredQuery, candidateMvps, investorFundName)

        return InvestorSearchResult(
            query = structuredQuery,
            rankedResults = ranked,
            executionLatencyMs = System.currentTimeMillis() - startTime,
            rawResponse = rawApiResponse
        )
    }

    /**
     * Deterministic Query Parser (Acceptance Test T2 & offline support).
     */
    fun executeDeterministicQueryParsing(utterance: String): StructuredSearchQuery {
        val lower = utterance.lowercase(Locale.ROOT)

        // 1. Parse INR bounds using InrParser
        val inrResult = InrParser.parse(utterance)
        val minInr = inrResult.minInr ?: inrResult.singleAmountInr
        val maxInr = inrResult.maxInr ?: inrResult.singleAmountInr

        // 2. Detect Target Sector & Synonyms
        val (sector, synonyms) = detectSectorAndSynonyms(lower)

        // 3. Extract Keywords
        val stopWords = setOf("i", "want", "a", "list", "of", "all", "apps", "mvps", "in", "where", "the", "investment", "amount", "is", "between", "and", "to", "cr", "crore", "lakh")
        val words = lower.split(" ", ",", ".", "?")
            .map { it.trim() }
            .filter { it.length > 2 && it !in stopWords }

        return StructuredSearchQuery(
            originalUtterance = utterance,
            targetSector = sector,
            sectorSynonyms = synonyms,
            minRaiseInr = minInr,
            maxRaiseInr = maxInr,
            keywords = words
        )
    }

    private fun detectSectorAndSynonyms(lower: String): Pair<String?, List<String>> {
        return when {
            lower.contains("hospital") || lower.contains("clinic") || lower.contains("doctor") ||
                    lower.contains("ehr") || lower.contains("emr") || lower.contains("health") || lower.contains("medical") ||
                    lower.contains("अस्पताल") || lower.contains("रुग्णालय") || lower.contains("மருத்துவமனை") ||
                    lower.contains("క్లినిక్") || lower.contains("ಆಸ್ಪತ್ರೆ") || lower.contains("হাসপাতাল") ||
                    lower.contains("clínica") || lower.contains("hospital") -> {
                "HealthTech & Hospital Management" to listOf("hospital", "clinic", "ehr", "emr", "health", "doctor", "medical", "patient", "clinic management", "अस्पताल", "रुग्णालय")
            }
            lower.contains("fintech") || lower.contains("pay") || lower.contains("upi") || lower.contains("bank") ||
                    lower.contains("lending") || lower.contains("credit") || lower.contains("crypto") ||
                    lower.contains("भुगतान") || lower.contains("பணம்") || lower.contains("pagos") -> {
                "FinTech & Payments" to listOf("fintech", "payment", "upi", "banking", "lending", "wallet", "credit", "invoice")
            }
            lower.contains("campus") || lower.contains("college") || lower.contains("edtech") ||
                    lower.contains("student") || lower.contains("exam") || lower.contains("learn") ||
                    lower.contains("कॉलेज") || lower.contains("शिक्षण") || lower.contains("கல்வி") -> {
                "EdTech & Campus Tools" to listOf("campus", "college", "student", "edtech", "learning", "study", "university", "placement")
            }
            lower.contains("agri") || lower.contains("farm") || lower.contains("crop") || lower.contains("farmer") ||
                    lower.contains("किसान") || lower.contains("शेती") || lower.contains("விவசாயம்") -> {
                "AgriTech & Supply Chain" to listOf("agri", "farming", "crop", "farmer", "agriculture", "supply chain", "cold storage")
            }
            lower.contains("ev") || lower.contains("climate") || lower.contains("solar") || lower.contains("battery") -> {
                "ClimateTech & EV" to listOf("climate", "ev", "solar", "battery", "carbon", "renewable", "clean energy")
            }
            lower.contains("hardware") || lower.contains("robot") || lower.contains("iot") || lower.contains("drone") -> {
                "DeepTech & Hardware" to listOf("hardware", "iot", "robotics", "drone", "embedded", "sensors", "deeptech")
            }
            lower.contains("d2c") || lower.contains("consumer") || lower.contains("food") || lower.contains("commerce") -> {
                "Consumer & D2C" to listOf("d2c", "consumer", "food", "fashion", "retail", "quick commerce", "brand")
            }
            lower.contains("saas") || lower.contains("ai") || lower.contains("devtools") || lower.contains("agent") -> {
                "AI, SaaS & DevTools" to listOf("ai", "saas", "devtools", "agent", "software", "b2b", "workflow")
            }
            else -> null to emptyList()
        }
    }

    private fun rankMvps(
        query: StructuredSearchQuery,
        candidateMvps: List<MvpPitchRecord>,
        investorFundName: String
    ): List<RankedMvpResult> {
        val rankedList = candidateMvps.map { mvp ->
            var score = 0.0
            val matchReasons = mutableListOf<String>()

            // 1. Sector matching (weight: 40%)
            if (query.targetSector != null) {
                if (mvp.sector.equals(query.targetSector, ignoreCase = true)) {
                    score += 0.40
                    matchReasons.add("Sector matched: ${mvp.sector}")
                } else if (query.sectorSynonyms.any { mvp.description.contains(it, ignoreCase = true) || mvp.appName.contains(it, ignoreCase = true) }) {
                    score += 0.30
                    matchReasons.add("Matched sector synonyms in pitch")
                }
            } else {
                score += 0.20 // Neutral if no sector specified
            }

            // 2. Raise Amount Range Matching (weight: 40%)
            val raise = mvp.raiseAmountInr
            if (query.minRaiseInr != null && query.maxRaiseInr != null) {
                if (raise in query.minRaiseInr..query.maxRaiseInr) {
                    score += 0.40
                    matchReasons.add("Raise amount (₹${InrParser.formatInr(raise)}) is within target range")
                } else {
                    val distance = minOf(
                        kotlin.math.abs(raise - query.minRaiseInr),
                        kotlin.math.abs(raise - query.maxRaiseInr)
                    )
                    if (distance <= 5_000_000L) { // Close by 50L
                        score += 0.15
                        matchReasons.add("Raise amount near target range boundary")
                    }
                }
            } else if (query.minRaiseInr != null && raise >= query.minRaiseInr) {
                score += 0.40
                matchReasons.add("Raise amount meets minimum threshold")
            } else if (query.maxRaiseInr != null && raise <= query.maxRaiseInr) {
                score += 0.40
                matchReasons.add("Raise amount within maximum ceiling")
            } else {
                score += 0.20 // Neutral if no amount specified
            }

            // 3. Keyword Relevance (weight: 20%)
            val hitKeywords = query.keywords.filter { kw ->
                mvp.appName.contains(kw, ignoreCase = true) ||
                        mvp.description.contains(kw, ignoreCase = true) ||
                        mvp.sector.contains(kw, ignoreCase = true)
            }
            if (hitKeywords.isNotEmpty()) {
                val kwFraction = minOf(1.0, hitKeywords.size / 2.0)
                score += 0.20 * kwFraction
                matchReasons.add("Keywords: ${hitKeywords.joinToString(", ")}")
            }

            // Generate clean one-line AI summary
            val summary = mvp.oneLineAiSummary
                ?: "${mvp.appName}: ${mvp.stage} startup in ${mvp.sector} solving ${mvp.description.take(60)}..."

            // Draft intro message for "Contact student" action
            val introMsg = draftIntroMessage(mvp, investorFundName)

            RankedMvpResult(
                mvp = mvp,
                relevanceScore = (score * 100).toInt() / 100.0,
                oneLineAiSummary = summary,
                matchReasons = matchReasons,
                draftedIntroMessage = introMsg
            )
        }

        // Sort descending by relevance score
        return rankedList.sortedByDescending { it.relevanceScore }
    }

    /**
     * Introduction message generator.
     */
    fun draftIntroMessage(mvp: MvpPitchRecord, investorFundName: String): String {
        val raiseStr = if (mvp.raiseAmountInr > 0L) "₹${InrParser.formatInr(mvp.raiseAmountInr)}" else "your current funding round"
        return "Hi ${mvp.appName} team! We at $investorFundName reviewed your MVP on CampusPitch and are interested in your solution for ${mvp.sector}. We would like to discuss your raise of $raiseStr. Are you open for a quick introductory video call this week?"
    }

    private fun buildSearchPrompt(utterance: String): String {
        return """
            You are the InvestorSearchAgent for CampusPitch. Extract structured search query filters from the investor's sentence into strict JSON.
            
            Canonical Sectors:
            ${AppConstants.CANONICAL_SECTORS.joinToString("\n") { "- $it" }}
            
            Indian Currency Rules:
            - 1 cr = 10,000,000 INR
            - 5 cr = 50,000,000 INR
            - 50 lakh = 5,000,000 INR
            
            Return JSON format:
            {
              "targetSector": "...",
              "sectorSynonyms": ["hospital", "clinic", ...],
              "minRaiseInr": 10000000,
              "maxRaiseInr": 50000000,
              "keywords": ["hospital", "management", ...]
            }
            
            Utterance: "$utterance"
        """.trimIndent()
    }

    private fun parseGeminiSearchJson(jsonString: String, originalUtterance: String): StructuredSearchQuery? {
        return try {
            val cleanJson = jsonString
                .substringAfter("{")
                .substringBeforeLast("}")
            val fullJson = "{$cleanJson}"
            val obj = JSONObject(fullJson)

            val sector = obj.optString("targetSector").takeIf { it.isNotBlank() && it != "null" }
            val minInr = if (obj.has("minRaiseInr")) obj.optLong("minRaiseInr").takeIf { it > 0 } else null
            val maxInr = if (obj.has("maxRaiseInr")) obj.optLong("maxRaiseInr").takeIf { it > 0 } else null

            val synArray = obj.optJSONArray("sectorSynonyms")
            val synonyms = mutableListOf<String>()
            if (synArray != null) {
                for (i in 0 until synArray.length()) {
                    synonyms.add(synArray.getString(i))
                }
            }

            val kwArray = obj.optJSONArray("keywords")
            val keywords = mutableListOf<String>()
            if (kwArray != null) {
                for (i in 0 until kwArray.length()) {
                    keywords.add(kwArray.getString(i))
                }
            }

            StructuredSearchQuery(
                originalUtterance = originalUtterance,
                targetSector = sector,
                sectorSynonyms = synonyms,
                minRaiseInr = minInr,
                maxRaiseInr = maxInr,
                keywords = keywords
            )
        } catch (e: Exception) {
            null
        }
    }
}
