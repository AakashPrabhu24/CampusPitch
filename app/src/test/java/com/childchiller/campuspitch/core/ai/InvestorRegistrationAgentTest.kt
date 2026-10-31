package com.childchiller.campuspitch.core.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestorRegistrationAgentTest {

    private val agent = InvestorRegistrationAgent(generativeModel = null)

    @Test
    fun testAcceptanceTestT3_InvestorRegistration() = runBlocking {
        val input = "My fund is BluePeak Ventures and we are an AIF Category I. My official email is partner@bluepeak.vc, mobile 9876543210, website bluepeak.vc."
        val result = agent.processRegistrationSentence(input, "investor_test_uid")

        assertNotNull(result.profile)
        val profile = result.profile!!
        assertEquals("BluePeak Ventures", profile.fundName)
        assertEquals("AIF Category I", profile.aifCategory)
        assertEquals("partner@bluepeak.vc", profile.officialEmail)
        assertEquals("9876543210", profile.mobile)
        assertEquals("bluepeak.vc", profile.website)
        assertEquals("pending", profile.status)

        // Validations
        assertTrue("Email should be valid", result.validation.isEmailValid)
        assertTrue("Mobile should be valid", result.validation.isMobileValid)
        assertTrue("Website should be valid", result.validation.isWebsiteValid)
        assertTrue("Domain should match website", result.validation.doesEmailDomainMatchWebsite)
        assertTrue("Validation should be fully valid", result.validation.isFullyValid)
        assertTrue("Errors should be empty", result.validation.validationErrors.isEmpty())
    }

    @Test
    fun testMismatchedDomain_isFlaggedInValidation() = runBlocking {
        val input = "My fund is BluePeak Ventures and we are an AIF Category I. My official email is partner@gmail.com, mobile 9876543210, website bluepeak.vc."
        val result = agent.processRegistrationSentence(input, "investor_test_uid")

        assertNotNull(result.profile)
        assertFalse(result.validation.doesEmailDomainMatchWebsite)
        assertFalse(result.validation.isFullyValid)
        assertTrue(result.validation.validationErrors.any { it.contains("must match fund website domain") })
    }

    @Test
    fun testInvalidMobile_isFlaggedInValidation() = runBlocking {
        val input = "My fund is BluePeak Ventures and we are an AIF Category I. My official email is partner@bluepeak.vc, mobile 12345, website bluepeak.vc."
        val result = agent.processRegistrationSentence(input, "investor_test_uid")

        assertNotNull(result.profile)
        assertFalse(result.validation.isMobileValid)
        assertFalse(result.validation.isFullyValid)
        assertTrue(result.validation.validationErrors.any { it.contains("Mobile number must be a 10-digit Indian number") })
    }

    @Test
    fun testInvestorCodeGeneration() {
        val code = agent.generateInvestorCode("BluePeak Ventures")
        assertTrue("Code must start with CCV-", code.startsWith("CCV-BPV-"))
        assertEquals(12, code.length)
    }
}
