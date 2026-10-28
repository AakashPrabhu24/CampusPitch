package com.childchiller.campuspitch.core.models

/**
 * Central Data Lake Architecture
 *
 * Zones:
 * 1. Raw Zone: `raw_inputs` (original voice/text, redacted transcript, timestamp, modelVersion)
 * 2. Curated Zone: `users`, `students`, `mvps`, `investors`, `outreach`, `funding`
 * 3. Audit Zone: `agent_runs` (every AI agent invocation, latency, confidence, tool calls)
 */

data class RawInputRecord(
    val id: String = "",
    val uid: String = "",
    val inputChannel: String = "voice_multilingual", // 'voice_multilingual' | 'voice_en_IN' | 'text' | 'github_api' | 'file_saf'
    val rawTranscriptRedacted: String = "", // Original transcript in native script (Aadhaar-redacted)
    val transcriptEnglish: String = "", // Translated English transcript for Data Lake indexing
    val languageCode: String = "en-IN", // BCP-47 tag (e.g. hi-IN, mr-IN, ta-IN, es-ES)
    val languageName: String = "English",
    val modelVersion: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "uid" to uid,
        "inputChannel" to inputChannel,
        "rawTranscriptRedacted" to rawTranscriptRedacted,
        "transcriptEnglish" to transcriptEnglish,
        "languageCode" to languageCode,
        "languageName" to languageName,
        "modelVersion" to modelVersion,
        "createdAt" to createdAt
    )
}

data class AgentRunRecord(
    val id: String = "",
    val uid: String = "",
    val agentName: String = "",
    val rawInputId: String = "",
    val modelVersion: String = "",
    val structuredOutput: Map<String, Any?> = emptyMap(),
    val fieldConfidence: Map<String, Double> = emptyMap(),
    val userConfirmed: Boolean = false,
    val latencyMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "uid" to uid,
        "agentName" to agentName,
        "rawInputId" to rawInputId,
        "modelVersion" to modelVersion,
        "structuredOutput" to structuredOutput,
        "fieldConfidence" to fieldConfidence,
        "userConfirmed" to userConfirmed,
        "latencyMs" to latencyMs,
        "timestamp" to timestamp
    )
}

data class AppUserRecord(
    val uid: String = "",
    val email: String = "",
    val role: String = "student", // 'student' | 'investor' | 'admin'
    val ageVerified18Plus: Boolean = true,
    val privacyConsentGranted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "email" to email,
        "role" to role,
        "ageVerified18Plus" to ageVerified18Plus,
        "privacyConsentGranted" to privacyConsentGranted,
        "createdAt" to createdAt
    )
}

data class StudentProfileRecord(
    val uid: String = "",
    val studentName: String? = null,
    val collegeName: String? = null,
    val collegeId: String? = null,
    val aadhaarSaltedHash: String? = null,
    val aadhaarLast4: String? = null,
    val mobile: String? = null,
    val contactShareConsent: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "studentName" to studentName,
        "collegeName" to collegeName,
        "collegeId" to collegeId,
        "aadhaarSaltedHash" to aadhaarSaltedHash,
        "aadhaarLast4" to aadhaarLast4,
        "mobile" to mobile,
        "contactShareConsent" to contactShareConsent,
        "updatedAt" to updatedAt
    )
}

data class MvpPitchRecord(
    val id: String = "",
    val ownerUid: String = "",
    val studentUid: String = ownerUid, // Backward-compatible alias
    val ownerName: String = "Campus Founder",
    val ownerCollege: String = "Campus Network",
    val appName: String = "",
    val description: String = "",
    val sector: String = "",
    val tags: List<String> = emptyList(),
    val subSector: String? = null,
    val stage: String = "MVP", // 'Idea' | 'Prototype' | 'MVP' | 'Early Revenue'
    val raiseAmountInr: Long = 0L,
    val raiseOriginalWording: String = "",
    val links: List<String> = emptyList(),
    val status: String = "published", // 'draft' | 'published'
    val oneLineAiSummary: String? = null,
    val fieldConfidence: Map<String, Double> = emptyMap(),
    val rawInputId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val name: String get() = appName
    val raiseText: String get() = raiseOriginalWording

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUid" to (if (ownerUid.isNotBlank()) ownerUid else studentUid),
        "studentUid" to (if (studentUid.isNotBlank()) studentUid else ownerUid),
        "ownerName" to ownerName,
        "ownerCollege" to ownerCollege,
        "appName" to appName,
        "name" to appName,
        "description" to description,
        "sector" to sector,
        "tags" to tags,
        "subSector" to subSector,
        "stage" to stage,
        "raiseAmountInr" to raiseAmountInr,
        "raiseOriginalWording" to raiseOriginalWording,
        "raiseText" to raiseOriginalWording,
        "links" to links,
        "status" to status,
        "oneLineAiSummary" to oneLineAiSummary,
        "fieldConfidence" to fieldConfidence,
        "rawInputId" to rawInputId,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

data class InvestorProfileRecord(
    val uid: String = "",
    val fundName: String = "",
    val aifCategory: String = "AIF Category I", // 'AIF Category I' | 'AIF Category II' | 'AIF Category III' | 'VC / Angel'
    val officialEmail: String = "",
    val mobile: String = "",
    val website: String = "",
    val emailDomainMatchesWebsite: Boolean = true,
    val status: String = "pending", // 'pending' | 'verified' | 'approved' | 'rejected'
    val issuedInvestorCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isVerified: Boolean
        get() = status.equals("verified", ignoreCase = true) || status.equals("approved", ignoreCase = true)

    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "fundName" to fundName,
        "aifCategory" to aifCategory,
        "officialEmail" to officialEmail,
        "mobile" to mobile,
        "website" to website,
        "emailDomainMatchesWebsite" to emailDomainMatchesWebsite,
        "status" to status,
        "issuedInvestorCode" to issuedInvestorCode,
        "createdAt" to createdAt
    )
}

data class OutreachRecord(
    val id: String = "",
    val investorUid: String = "",
    val investorFundName: String = "",
    val studentUid: String = "",
    val mvpId: String = "",
    val mvpAppName: String = "",
    val introMessage: String = "",
    val status: String = "Contacted", // 'Contacted' | 'Replied' | 'Meeting' | 'Term sheet' | 'Funded' | 'Declined'
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "investorUid" to investorUid,
        "investorFundName" to investorFundName,
        "studentUid" to studentUid,
        "mvpId" to mvpId,
        "mvpAppName" to mvpAppName,
        "introMessage" to introMessage,
        "status" to status,
        "updatedAt" to updatedAt
    )
}

data class FundingRecord(
    val id: String = "",
    val studentUid: String = "",
    val investorUid: String = "",
    val mvpId: String = "",
    val amountInr: Long = 0L,
    val termSheetDate: Long = System.currentTimeMillis(),
    val status: String = "Term sheet",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "studentUid" to studentUid,
        "investorUid" to investorUid,
        "mvpId" to mvpId,
        "amountInr" to amountInr,
        "termSheetDate" to termSheetDate,
        "status" to status,
        "updatedAt" to updatedAt
    )
}

/**
 * Two-way messaging record connecting student founders and institutional investors.
 * Persisted in Firestore under `outreach/{conversationId}/messages/{messageId}`.
 */
data class ChatMessageRecord(
    val id: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "student", // 'student' | 'investor'
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "conversationId" to conversationId,
        "senderUid" to senderUid,
        "senderName" to senderName,
        "senderRole" to senderRole,
        "text" to text,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ChatMessageRecord {
            return ChatMessageRecord(
                id = map["id"] as? String ?: "",
                conversationId = map["conversationId"] as? String ?: "",
                senderUid = map["senderUid"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                senderRole = map["senderRole"] as? String ?: "student",
                text = map["text"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
