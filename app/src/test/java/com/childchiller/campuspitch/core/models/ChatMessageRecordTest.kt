package com.childchiller.campuspitch.core.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageRecordTest {

    @Test
    fun testSerializationAndDeserialization() {
        val original = ChatMessageRecord(
            id = "msg-123",
            conversationId = "conv-abc",
            senderUid = "user-investor-456",
            senderName = "Child Chiller Growth Fund",
            senderRole = "investor",
            text = "We are interested in evaluating your MediCare MVP. Let's schedule a call.",
            timestamp = 1714000000000L
        )

        val map = original.toMap()
        assertEquals("msg-123", map["id"])
        assertEquals("conv-abc", map["conversationId"])
        assertEquals("user-investor-456", map["senderUid"])
        assertEquals("Child Chiller Growth Fund", map["senderName"])
        assertEquals("investor", map["senderRole"])
        assertEquals("We are interested in evaluating your MediCare MVP. Let's schedule a call.", map["text"])
        assertEquals(1714000000000L, map["timestamp"])

        val deserialized = ChatMessageRecord.fromMap(map)
        assertEquals(original.id, deserialized.id)
        assertEquals(original.conversationId, deserialized.conversationId)
        assertEquals(original.senderUid, deserialized.senderUid)
        assertEquals(original.senderName, deserialized.senderName)
        assertEquals(original.senderRole, deserialized.senderRole)
        assertEquals(original.text, deserialized.text)
        assertEquals(original.timestamp, deserialized.timestamp)
    }

    @Test
    fun testStudentReplyMessage() {
        val reply = ChatMessageRecord(
            id = "msg-student-789",
            conversationId = "conv-abc",
            senderUid = "student-founder-101",
            senderName = "Aarav Sharma",
            senderRole = "student",
            text = "Thank you! I am available tomorrow between 3 PM and 6 PM IST.",
            timestamp = 1714000060000L
        )

        assertEquals("student", reply.senderRole)
        assertEquals("student-founder-101", reply.senderUid)
        assertTrue(reply.text.contains("tomorrow"))
    }

    @Test
    fun testFounderPrivacyConsentHandling() {
        // When founder has not consented to external contact sharing
        val privateProfile = StudentProfileRecord(
            uid = "student-1",
            studentName = "Priya Patel",
            contactShareConsent = false
        )
        assertFalse("Consent must be false by default", privateProfile.contactShareConsent)

        // When founder grants consent
        val consentedProfile = privateProfile.copy(contactShareConsent = true)
        assertTrue("Consent granted when explicitly approved", consentedProfile.contactShareConsent)
    }
}
