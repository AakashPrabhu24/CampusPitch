package com.childchiller.campuspitch.core.utils

import com.childchiller.campuspitch.core.constants.AppConstants
import java.security.MessageDigest

/**
 * Privacy & Security utilities for Student Aadhaar & College ID.
 * Strict compliance: Never stores or logs raw 12-digit Aadhaar.
 */
data class MaskedIdentityResult(
    val identityType: String, // "college_id" or "aadhaar"
    val collegeId: String? = null,
    val aadhaarSaltedHash: String? = null,
    val aadhaarLast4: String? = null,
    val displayMask: String
)

object AadhaarCrypto {

    /**
     * Normalizes spoken English/Hinglish digits ("zero", "one", "two" ...) into numeric string.
     */
    fun normalizeDigits(input: String): String {
        var text = input.lowercase().trim()
        val wordToDigit = mapOf(
            "zero" to "0", "shunya" to "0", "oh" to "0",
            "one" to "1", "ek" to "1",
            "two" to "2", "do" to "2",
            "three" to "3", "teen" to "3",
            "four" to "4", "chaar" to "4",
            "five" to "5", "paanch" to "5",
            "six" to "6", "chhe" to "6",
            "seven" to "7", "saat" to "7",
            "eight" to "8", "aath" to "8",
            "nine" to "9", "nau" to "9"
        )
        wordToDigit.forEach { (word, digit) ->
            text = text.replace(Regex("\\b$word\\b"), digit)
        }
        return text.replace(Regex("[^0-9]"), "")
    }

    /**
     * UIDAI standards: 12 numeric digits, does not start with '0' or '1'.
     */
    fun isValidAadhaarDigits(digits: String): Boolean {
        if (digits.length != 12) return false
        val firstChar = digits[0]
        return firstChar != '0' && firstChar != '1'
    }

    /**
     * Hashes the 12-digit Aadhaar with a salt + userUid using SHA-256.
     * Retains ONLY the salted hash and last 4 digits for display.
     */
    fun hashAadhaar(rawInput: String, userUid: String): MaskedIdentityResult {
        val digits = normalizeDigits(rawInput)
        require(isValidAadhaarDigits(digits)) {
            "Aadhaar must be exactly 12 digits and cannot start with 0 or 1"
        }
        val last4 = digits.substring(8, 12)
        val payload = "${AppConstants.AADHAAR_APPLICATION_SALT}:$userUid:$digits"
        val md = MessageDigest.getInstance("SHA-256")
        val digestBytes = md.digest(payload.toByteArray(Charsets.UTF_8))
        val hashHex = digestBytes.joinToString("") { "%02x".format(it) }

        return MaskedIdentityResult(
            identityType = "aadhaar",
            aadhaarSaltedHash = hashHex,
            aadhaarLast4 = last4,
            displayMask = "XXXX-XXXX-$last4"
        )
    }

    /**
     * Parses college ID string.
     */
    fun parseCollegeId(rawInput: String): MaskedIdentityResult {
        val cleaned = rawInput.trim()
        require(cleaned.isNotEmpty()) { "College ID cannot be empty" }
        return MaskedIdentityResult(
            identityType = "college_id",
            collegeId = cleaned,
            displayMask = cleaned
        )
    }

    /**
     * Redacts any potential 12-digit Aadhaar numbers from raw voice transcripts
     * before persisting to the data lake raw zone.
     */
    fun redactAadhaarFromTranscript(transcript: String): String {
        val aadhaarPattern = Regex("\\b[2-9][0-9]{3}\\s?[0-9]{4}\\s?[0-9]{4}\\b")
        return transcript.replace(aadhaarPattern) { matchResult ->
            val clean = matchResult.value.replace(Regex("\\s+"), "")
            val last4 = clean.takeLast(4)
            "[AADHAAR-REDACTED-$last4]"
        }
    }
}
