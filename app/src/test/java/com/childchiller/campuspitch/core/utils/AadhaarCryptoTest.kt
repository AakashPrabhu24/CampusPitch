package com.childchiller.campuspitch.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AadhaarCryptoTest {

    @Test
    fun testValidAadhaarHashingAndMasking() {
        val rawInput = "5432 1098 7654"
        val userUid = "test_user_uid_123"
        val result = AadhaarCrypto.hashAadhaar(rawInput, userUid)

        assertEquals("aadhaar", result.identityType)
        assertEquals("7654", result.aadhaarLast4)
        assertEquals("XXXX-XXXX-7654", result.displayMask)
        assertTrue(result.aadhaarSaltedHash != null && result.aadhaarSaltedHash!!.length == 64) // SHA-256 hex
        assertFalse(result.aadhaarSaltedHash!!.contains("5432"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectInvalidAadhaarStartingWithZero() {
        AadhaarCrypto.hashAadhaar("0123 4567 8901", "uid_1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectInvalidAadhaarWrongLength() {
        AadhaarCrypto.hashAadhaar("12345678", "uid_1")
    }

    @Test
    fun testSpokenDigitNormalization() {
        val spoken = "five four three two one zero nine eight seven six five four"
        val normalized = AadhaarCrypto.normalizeDigits(spoken)
        assertEquals("543210987654", normalized)
    }

    @Test
    fun testRedactAadhaarFromTranscript() {
        val transcript = "Hello my Aadhaar is 5432 1098 7654 and my app is MediTrack"
        val redacted = AadhaarCrypto.redactAadhaarFromTranscript(transcript)

        assertFalse(redacted.contains("5432 1098"))
        assertTrue(redacted.contains("[AADHAAR-REDACTED-7654]"))
        assertTrue(redacted.contains("MediTrack"))
    }
}
