package com.childchiller.campuspitch.core.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Deterministic Indian Currency (INR) and Multilingual / Hinglish Range Parser.
 *
 * Conversions:
 * - 1 Crore (cr / crore / crores / koti / kodi / khokha / करोड़ / कोटी / கோடி / కోటి) = 1,00,00,000 INR (10,000,000)
 * - 1 Lakh (lakh / lakhs / lac / lacs / latcham / laksha / peti / लाख / லட்சம் / లక్ష) = 1,00,00,000 INR (100,000)
 * - 1 Thousand (k / thousand / hazaar / हजार / ஆயிரம்) = 1,000 INR
 */
data class InrParseResult(
    val minInr: Long? = null,
    val maxInr: Long? = null,
    val singleAmountInr: Long? = null,
    val originalWording: String = ""
) {
    val isRange: Boolean
        get() = minInr != null && maxInr != null && minInr != maxInr

    val formattedDisplay: String
        get() = when {
            isRange -> "₹${InrParser.formatInr(minInr!!)} - ₹${InrParser.formatInr(maxInr!!)}"
            singleAmountInr != null -> "₹${InrParser.formatInr(singleAmountInr)}"
            else -> "N/A"
        }
}

object InrParser {
    const val ONE_CRORE: Long = 10_000_000L
    const val ONE_LAKH: Long = 100_000L
    const val ONE_THOUSAND: Long = 1_000L

    /**
     * Formats an integer amount in Indian numbering format (e.g. 1,00,00,000).
     */
    fun formatInr(amount: Long): String {
        return try {
            val str = amount.toString()
            if (str.length <= 3) return str
            val lastThree = str.takeLast(3)
            val rest = str.dropLast(3)
            val formattedRest = rest.reversed().chunked(2).joinToString(",").reversed()
            "$formattedRest,$lastThree"
        } catch (e: Exception) {
            amount.toString()
        }
    }

    /**
     * Pre-normalizes spoken words, fractions, and multilingual / Hinglish slang into numerical strings.
     */
    private fun preprocessText(input: String): String {
        var s = input.lowercase().trim()
        s = s.replace("₹", " ")
            .replace("rs.", " ")
            .replace("rs", " ")
            .replace("inr", " ")
            .replace(",", "")

        // Fractional idioms in Indian English and Hinglish
        val idioms = listOf(
            Regex("\\bone and a half\\b") to "1.5",
            Regex("\\bone and half\\b") to "1.5",
            Regex("\\btwo and a half\\b") to "2.5",
            Regex("\\btwo and half\\b") to "2.5",
            Regex("\\bthree and a half\\b") to "3.5",
            Regex("\\bhalf a\\b") to "0.5",
            Regex("\\bhalf\\b(?=\\s*(cr|crore|crores|koti|kodi|lakh|lakhs|lac|lacs))") to "0.5",
            Regex("\\bdedh\\b") to "1.5",
            Regex("\\bdhai\\b") to "2.5",
            Regex("\\bsawa\\b") to "1.25",
            Regex("\\bpaune do\\b") to "1.75",
            Regex("\\bsaadhe teen\\b") to "3.5",
            Regex("\\bsaadhe chaar\\b") to "4.5",
            Regex("\\bsaadhe paanch\\b") to "5.5"
        )
        idioms.forEach { (pattern, replacement) ->
            s = s.replace(pattern, replacement)
        }

        // Spoken number words across English, Hinglish, Hindi, Marathi, Tamil, Spanish
        val wordNumbers = listOf(
            // English / Hindi / Hinglish / Marathi
            Regex("\\bone\\b") to "1", Regex("\\bek\\b") to "1", Regex("\\bएक\\b") to "1", Regex("\\bएका\\b") to "1",
            Regex("\\btwo\\b") to "2", Regex("\\bdo\\b") to "2", Regex("\\bदो\\b") to "2", Regex("\\bदोन\\b") to "2", Regex("\\bdon\\b") to "2",
            Regex("\\bthree\\b") to "3", Regex("\\bteen\\b") to "3", Regex("\\bतीन\\b") to "3",
            Regex("\\bfour\\b") to "4", Regex("\\bchaar\\b") to "4", Regex("\\bचार\\b") to "4",
            Regex("\\bfive\\b") to "5", Regex("\\bpaanch\\b") to "5", Regex("\\bपांच\\b") to "5", Regex("\\bपाच\\b") to "5", Regex("\\bpaach\\b") to "5",
            Regex("\\bsix\\b") to "6", Regex("\\bchhe\\b") to "6", Regex("\\bछह\\b") to "6", Regex("\\bसहा\\b") to "6",
            Regex("\\bseven\\b") to "7", Regex("\\bsaat\\b") to "7", Regex("\\bसात\\b") to "7",
            Regex("\\beight\\b") to "8", Regex("\\baath\\b") to "8", Regex("\\bआठ\\b") to "8",
            Regex("\\bnine\\b") to "9", Regex("\\bnau\\b") to "9", Regex("\\bनौ\\b") to "9", Regex("\\bनऊ\\b") to "9",
            Regex("\\bten\\b") to "10", Regex("\\bdas\\b") to "10", Regex("\\bदस\\b") to "10", Regex("\\bदहा\\b") to "10",
            Regex("\\bfifteen\\b") to "15", Regex("\\bpandra\\b") to "15", Regex("\\bपंद्रह\\b") to "15", Regex("\\bपंधरा\\b") to "15",
            Regex("\\btwenty\\b") to "20", Regex("\\bbees\\b") to "20", Regex("\\bबीस\\b") to "20", Regex("\\bवीस\\b") to "20",
            Regex("\\btwenty five\\b") to "25", Regex("\\bpachees\\b") to "25", Regex("\\bपच्चीस\\b") to "25", Regex("\\bपंचवीस\\b") to "25",
            Regex("\\bthirty\\b") to "30", Regex("\\btees\\b") to "30", Regex("\\bतीस\\b") to "30",
            Regex("\\bforty\\b") to "40", Regex("\\bchaalis\\b") to "40", Regex("\\bचालीस\\b") to "40", Regex("\\bचाळीस\\b") to "40",
            Regex("\\bfifty\\b") to "50", Regex("\\bpachaas\\b") to "50", Regex("\\bपचास\\b") to "50", Regex("\\bपन्नास\\b") to "50", Regex("\\bpannaas\\b") to "50",
            Regex("\\bseventy five\\b") to "75", Regex("\\bpachattar\\b") to "75",
            Regex("\\bhundred\\b") to "100", Regex("\\bsau\\b") to "100", Regex("\\bशंभर\\b") to "100",

            // Tamil
            Regex("\\bஒரு\\b") to "1", Regex("\\bஒன்று\\b") to "1", Regex("\\boru\\b") to "1",
            Regex("\\bஇரண்டு\\b") to "2", Regex("\\birandu\\b") to "2",
            Regex("\\bமூன்று\\b") to "3", Regex("\\bmoondru\\b") to "3",
            Regex("\\bநான்கு\\b") to "4", Regex("\\bnaangu\\b") to "4",
            Regex("\\bஐந்து\\b") to "5", Regex("\\baindhu\\b") to "5",
            Regex("\\bஐம்பது\\b") to "50", Regex("\\baimbathu\\b") to "50",

            // Spanish
            Regex("\\bun\\b") to "1", Regex("\\buno\\b") to "1", Regex("\\buna\\b") to "1",
            Regex("\\bdos\\b") to "2", Regex("\\btres\\b") to "3", Regex("\\bcuatro\\b") to "4",
            Regex("\\bcinco\\b") to "5", Regex("\\bdiez\\b") to "10", Regex("\\bcincuenta\\b") to "50",

            // Range connectors in multiple languages mapped to standard tokens
            Regex("\\bसे\\b") to "to", Regex("\\bते\\b") to "to", Regex("\\bவரை\\b") to "to", Regex("\\bமுதல்\\b") to "between",
            Regex("\\bentre\\b") to "between", Regex("\\bhasta\\b") to "to", Regex("\\ba\\b") to "to"
        )
        wordNumbers.forEach { (pattern, digit) ->
            s = s.replace(pattern, digit)
        }

        return s
    }

    private fun unitMultiplier(unit: String?): Long {
        if (unit == null) return 1L
        val u = unit.lowercase().trim()
        return when {
            u.startsWith("cr") || u.startsWith("crore") || u.startsWith("koti") || u.startsWith("kodi") ||
                    u == "khokha" || u.contains("करोड़") || u.contains("कोटी") || u.contains("கோடி") || u.contains("కోటి") -> ONE_CRORE
            u.startsWith("lakh") || u.startsWith("lac") || u == "l" || u == "peti" || u.startsWith("latcham") ||
                    u.startsWith("laksha") || u.contains("लाख") || u.contains("லட்சம்") || u.contains("లక్ష") -> ONE_LAKH
            u.startsWith("k") || u.startsWith("thousand") || u == "hazaar" || u.contains("हजार") || u.contains("ஆயிரம்") -> ONE_THOUSAND
            u.contains("millón") || u.contains("millones") || u.contains("million") -> 1_000_000L
            else -> 1L
        }
    }

    private fun parseAmount(numStr: String, unitStr: String?): Long {
        val num = numStr.toDoubleOrNull() ?: 0.0
        val multiplier = unitMultiplier(unitStr)
        return (num * multiplier).toLong()
    }

    /**
     * Parses spoken or typed text for single amounts or ranges across Indian & international terms.
     * Examples:
     * - "1 cr" -> single: 10,000,000
     * - "50 lakh" -> single: 5,000,000
     * - "one and a half crore" -> single: 15,000,000
     * - "between 1 cr and 5 cr" -> min: 10,000,000, max: 50,000,000
     * - "1 to 5 crore" -> min: 10,000,000, max: 50,000,000
     * - "1 कोटी" -> single: 10,000,000
     * - "1 ते 5 कोटी" -> min: 10,000,000, max: 50,000,000
     * - "1 கோடி" -> single: 10,000,000
     */
    fun parse(input: String): InrParseResult {
        if (input.isBlank()) {
            return InrParseResult(originalWording = input)
        }

        val cleaned = preprocessText(input)

        // Range pattern: "between X [unit] and Y [unit]" or "X [unit]? to Y [unit]" or "X - Y [unit]"
        val unitPattern = "(cr|crore|crores|koti|kodi|lakh|lakhs|lac|lacs|latcham|laksha|l|k|thousand|hazaar|khokha|peti|करोड़|कोटी|लाख|கோடி|லட்சம்|millón|millones)"
        val rangeRegex = Regex(
            "(?:between\\s+)?(\\d+(?:\\.\\d+)?)\\s*(?:$unitPattern)?\\s*(?:to|and|-)\\s*(\\d+(?:\\.\\d+)?)\\s*$unitPattern?",
            RegexOption.IGNORE_CASE
        )

        val rangeMatch = rangeRegex.find(cleaned)
        if (rangeMatch != null) {
            val num1 = rangeMatch.groupValues[1]
            val explicitUnit1 = rangeMatch.groups[2]?.value
            val num2 = rangeMatch.groupValues[3]
            val explicitUnit2 = rangeMatch.groups[4]?.value

            val unit2 = explicitUnit2 ?: explicitUnit1
            val unit1 = explicitUnit1 ?: unit2

            val minVal = parseAmount(num1, unit1)
            val maxVal = parseAmount(num2, unit2)

            return InrParseResult(
                minInr = minOf(minVal, maxVal),
                maxInr = maxOf(minVal, maxVal),
                originalWording = rangeMatch.value.trim()
            )
        }

        // Single amount pattern: "(\\d+(?:\\.\\d+)?)\\s*$unitPattern"
        val singleRegex = Regex(
            "(\\d+(?:\\.\\d+)?)\\s*$unitPattern",
            RegexOption.IGNORE_CASE
        )
        val singleMatch = singleRegex.find(cleaned)
        if (singleMatch != null) {
            val num = singleMatch.groupValues[1]
            val unit = singleMatch.groupValues[2]
            val amount = parseAmount(num, unit)
            return InrParseResult(
                singleAmountInr = amount,
                originalWording = singleMatch.value.trim()
            )
        }

        // Plain raw number fallback (e.g. "10000000")
        val rawNumberRegex = Regex("\\b(\\d{5,10})\\b")
        val rawMatch = rawNumberRegex.find(cleaned)
        if (rawMatch != null) {
            val amount = rawMatch.groupValues[1].toLongOrNull()
            if (amount != null) {
                return InrParseResult(
                    singleAmountInr = amount,
                    originalWording = input.trim()
                )
            }
        }

        return InrParseResult(originalWording = input.trim())
    }
}
