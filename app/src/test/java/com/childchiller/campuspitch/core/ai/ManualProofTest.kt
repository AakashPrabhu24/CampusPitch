package com.childchiller.campuspitch.core.ai

import com.childchiller.campuspitch.core.models.MvpPitchRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualProofTest {

    private val studentAgent = StudentIntakeAgent(generativeModel = null)
    private val investorAgent = InvestorSearchAgent(generativeModel = null)

    @Test
    fun testProofUserSentenceExtractedIntoSeparateFieldsAndSearchable() = runBlocking {
        val studentSentence = "I want to upload an app called MediCare. It manages hospitals, and I want to raise ₹2 crore."
        val result = studentAgent.processPitchSentence(studentSentence, "student_user_123")

        assertNotNull(result.draftMvp)
        val mvp = result.draftMvp!!

        println("=== PROOF: STRUCTURED EXTRACTION ===")
        println("Field 1 (appName): ${mvp.appName}")
        println("Field 2 (description): ${mvp.description}")
        println("Field 3 (sector): ${mvp.sector}")
        println("Field 4 (raiseAmountInr): ${mvp.raiseAmountInr} INR")
        println("Field 5 (raiseOriginalWording): ${mvp.raiseOriginalWording}")

        // 1. Verify exact separate fields
        assertEquals("MediCare", mvp.appName)
        assertEquals("It manages hospitals", mvp.description)
        assertEquals("HealthTech & Hospital Management", mvp.sector)
        assertEquals(20_000_000L, mvp.raiseAmountInr)
        assertTrue(mvp.raiseOriginalWording.contains("2"))

        // 2. Verify Firestore toMap() serializes to individual database columns/fields
        val map = mvp.toMap()
        assertEquals("MediCare", map["appName"])
        assertEquals("It manages hospitals", map["description"])
        assertEquals("HealthTech & Hospital Management", map["sector"])
        assertEquals(20_000_000L, map["raiseAmountInr"])
        assertTrue((map["raiseOriginalWording"] as String).contains("2"))

        // 3. Verify Natural Language Investor Search uses these separate fields
        val candidatePool = listOf(mvp)
        val searchUtterance = "Show me hospital management apps raising between 1 and 3 crore"
        val searchResult = investorAgent.executeSearch(searchUtterance, candidatePool, "Child Chiller Capital")

        assertTrue(searchResult.rankedResults.isNotEmpty())
        val topResult = searchResult.rankedResults.first()
        assertEquals("MediCare", topResult.mvp.appName)
        assertEquals(20_000_000L, topResult.mvp.raiseAmountInr)
        assertTrue(topResult.relevanceScore >= 0.8)

        println("=== PROOF: NATURAL LANGUAGE SEARCH ===")
        println("Investor Utterance: \"$searchUtterance\"")
        println("Matched MVP: ${topResult.mvp.appName} with score ${topResult.relevanceScore}")
        println("Match Reasons: ${topResult.matchReasons}")
    }
}
