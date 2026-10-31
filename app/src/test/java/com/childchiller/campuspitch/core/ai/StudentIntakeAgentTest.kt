package com.childchiller.campuspitch.core.ai

import com.childchiller.campuspitch.core.models.MvpPitchRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StudentIntakeAgentTest {

    private val agent = StudentIntakeAgent(generativeModel = null) // Runs deterministic engine

    @Test
    fun testAcceptanceTestT1_MediTrackExtraction() = runBlocking {
        val input = "I want to upload an app called MediTrack. It does hospital management for small clinics. I am looking to raise 1 cr to scale the app."
        val result = agent.processPitchSentence(input, "student_test_uid")

        assertNotNull(result.draftMvp)
        val mvp = result.draftMvp!!
        assertEquals("MediTrack", mvp.appName)
        assertEquals("hospital management for small clinics", mvp.description)
        assertEquals(10_000_000L, mvp.raiseAmountInr)
        assertEquals("1 cr", mvp.raiseOriginalWording)
        assertEquals("HealthTech & Hospital Management", mvp.sector)
        assertNull(result.missingEssentialField)
        assertNull(result.followUpQuestion)
    }

    @Test
    fun testMissingRaiseAmount_triggersFollowUpQuestion() = runBlocking {
        val input = "I want to upload an app called MediTrack. It does hospital management for small clinics."
        val result = agent.processPitchSentence(input, "student_test_uid")

        assertNotNull(result.draftMvp)
        assertEquals("MediTrack", result.draftMvp?.appName)
        assertEquals(0L, result.draftMvp?.raiseAmountInr)
        assertEquals("raiseAmountInr", result.missingEssentialField)
        assertNotNull(result.followUpQuestion)
        assertTrue(result.followUpQuestion!!.contains("How much capital are you looking to raise"))
    }

    @Test
    fun testVoiceEditUtterance_updatesRaiseAmount() = runBlocking {
        val initialDraft = MvpPitchRecord(
            id = UUID.randomUUID().toString(),
            studentUid = "student_test_uid",
            appName = "MediTrack",
            description = "hospital management for small clinics",
            sector = "HealthTech & Hospital Management",
            stage = "MVP",
            raiseAmountInr = 10_000_000L,
            raiseOriginalWording = "1 cr",
            updatedAt = System.currentTimeMillis()
        )

        val editInput = "change my raise to 2 cr"
        val result = agent.processPitchSentence(editInput, "student_test_uid", initialDraft)

        assertTrue(result.isVoiceEdit)
        assertNotNull(result.draftMvp)
        val updated = result.draftMvp!!
        assertEquals(20_000_000L, updated.raiseAmountInr)
        assertEquals("2 cr", updated.raiseOriginalWording)
        assertEquals("MediTrack", updated.appName)
    }
}
