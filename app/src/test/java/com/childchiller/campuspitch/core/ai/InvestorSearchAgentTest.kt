package com.childchiller.campuspitch.core.ai

import com.childchiller.campuspitch.core.models.MvpPitchRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class InvestorSearchAgentTest {

    private val agent = InvestorSearchAgent(generativeModel = null)

    @Test
    fun testAcceptanceTestT2_StructuredQueryParsing() {
        val utterance = "I want a list of all apps in Hospital Management systems where the investment amount is between 1 cr and 5 cr."
        val query = agent.executeDeterministicQueryParsing(utterance)

        assertEquals("HealthTech & Hospital Management", query.targetSector)
        assertEquals(10_000_000L, query.minRaiseInr)
        assertEquals(50_000_000L, query.maxRaiseInr)
        assertTrue(query.sectorSynonyms.contains("hospital"))
        assertTrue(query.sectorSynonyms.contains("clinic"))
    }

    @Test
    fun testRankedResults_PrioritizesMatchingSectorAndRange() = runBlocking {
        val mediTrack = MvpPitchRecord(
            id = UUID.randomUUID().toString(),
            appName = "MediTrack",
            description = "hospital management for small clinics",
            sector = "HealthTech & Hospital Management",
            stage = "MVP",
            raiseAmountInr = 10_000_000L,
            raiseOriginalWording = "1 cr"
        )

        val payFlow = MvpPitchRecord(
            id = UUID.randomUUID().toString(),
            appName = "PayFlow",
            description = "UPI recurring payment agent for campus vendors",
            sector = "FinTech & Payments",
            stage = "MVP",
            raiseAmountInr = 20_000_000L,
            raiseOriginalWording = "2 cr"
        )

        val agroSmart = MvpPitchRecord(
            id = UUID.randomUUID().toString(),
            appName = "AgroSmart",
            description = "Crop disease detection with smartphone camera",
            sector = "AgriTech & Supply Chain",
            stage = "Prototype",
            raiseAmountInr = 5_000_000L,
            raiseOriginalWording = "50 lakh"
        )

        val querySentence = "I want a list of all apps in Hospital Management systems where the investment amount is between 1 cr and 5 cr."
        val result = agent.executeSearch(querySentence, listOf(agroSmart, mediTrack, payFlow), "BluePeak Ventures")

        assertNotNull(result.rankedResults)
        assertEquals(3, result.rankedResults.size)

        val topResult = result.rankedResults.first()
        assertEquals("MediTrack", topResult.mvp.appName)
        assertTrue("MediTrack should have high relevance", topResult.relevanceScore >= 0.80)
        assertTrue(topResult.matchReasons.any { it.contains("Sector matched") })
        assertTrue(topResult.matchReasons.any { it.contains("within target range") })
    }

    @Test
    fun testIntroMessageDrafting() {
        val mediTrack = MvpPitchRecord(
            id = UUID.randomUUID().toString(),
            appName = "MediTrack",
            description = "hospital management for small clinics",
            sector = "HealthTech & Hospital Management",
            stage = "MVP",
            raiseAmountInr = 10_000_000L,
            raiseOriginalWording = "1 cr"
        )

        val intro = agent.draftIntroMessage(mediTrack, "BluePeak Ventures")
        assertTrue(intro.contains("BluePeak Ventures"))
        assertTrue(intro.contains("MediTrack"))
        assertTrue(intro.contains("HealthTech & Hospital Management"))
        assertTrue(intro.contains("₹1,00,00,000"))
    }
}
