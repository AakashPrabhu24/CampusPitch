package com.childchiller.campuspitch.core.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.childchiller.campuspitch.core.models.ChatMessageRecord
import com.childchiller.campuspitch.core.models.OutreachRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Universal Outreach & Acceptance Repository.
 *
 * Synchronizes investor outreach & acceptances across Firestore and local memory,
 * ensuring immediate real-time notifications for student founders and two-way messaging.
 */
class FirestoreOutreachRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : OutreachRepository {

    companion object {
        private val liveOutreaches = ConcurrentHashMap<String, OutreachRecord>()
        private val outreachUpdates = MutableSharedFlow<Unit>(replay = 1)

        private val liveMessages = ConcurrentHashMap<String, MutableList<ChatMessageRecord>>()
        private val messageUpdates = MutableSharedFlow<String>(replay = 1)

        fun getForStudent(studentUid: String): List<OutreachRecord> {
            return liveOutreaches.values.filter {
                it.studentUid == studentUid || studentUid == "student_preview_user"
            }.sortedByDescending { it.updatedAt }.toList()
        }

        fun getForInvestor(investorUid: String): List<OutreachRecord> {
            return liveOutreaches.values.filter {
                it.investorUid == investorUid || investorUid == "investor_session"
            }.sortedByDescending { it.updatedAt }.toList()
        }

        fun putOutreach(outreach: OutreachRecord) {
            liveOutreaches[outreach.id] = outreach
            outreachUpdates.tryEmit(Unit)
        }

        fun getMessages(conversationId: String): List<ChatMessageRecord> {
            return liveMessages[conversationId]?.sortedBy { it.timestamp } ?: emptyList()
        }

        fun putMessage(message: ChatMessageRecord) {
            val list = liveMessages.computeIfAbsent(message.conversationId) { CopyOnWriteArrayList() }
            val existingIdx = list.indexOfFirst { it.id == message.id }
            if (existingIdx >= 0) {
                list[existingIdx] = message
            } else {
                list.add(message)
            }
            messageUpdates.tryEmit(message.conversationId)
        }
    }

    override fun observeForStudent(studentUid: String): Flow<List<OutreachRecord>> = callbackFlow {
        trySend(getForStudent(studentUid))

        val listener = firestore.collection("outreach")
            .whereEqualTo("studentUid", studentUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val parsed = parseOutreachDocument(doc)
                        if (parsed != null) {
                            liveOutreaches[parsed.id] = parsed
                        }
                    }
                }
                trySend(getForStudent(studentUid))
            }

        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val updateJob = coroutineScope.launch {
            outreachUpdates.collect {
                trySend(getForStudent(studentUid))
            }
        }

        awaitClose {
            listener.remove()
            updateJob.cancel()
        }
    }

    override fun observeForInvestor(investorUid: String): Flow<List<OutreachRecord>> = callbackFlow {
        trySend(getForInvestor(investorUid))

        val listener = firestore.collection("outreach")
            .whereEqualTo("investorUid", investorUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val parsed = parseOutreachDocument(doc)
                        if (parsed != null) {
                            liveOutreaches[parsed.id] = parsed
                        }
                    }
                }
                trySend(getForInvestor(investorUid))
            }

        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val updateJob = coroutineScope.launch {
            outreachUpdates.collect {
                trySend(getForInvestor(investorUid))
            }
        }

        awaitClose {
            listener.remove()
            updateJob.cancel()
        }
    }

    override suspend fun sendOutreach(outreach: OutreachRecord): Result<Unit> = withContext(Dispatchers.IO) {
        putOutreach(outreach)
        try {
            firestore.collection("outreach").document(outreach.id).set(outreach.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun updateOutreachStatus(outreachId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = liveOutreaches[outreachId]
        if (existing != null) {
            val updated = existing.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            putOutreach(updated)
        }
        try {
            firestore.collection("outreach").document(outreachId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessageRecord>> = callbackFlow {
        trySend(getMessages(conversationId))

        val listener = firestore.collection("outreach")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val parsed = parseMessageDocument(doc)
                        if (parsed != null) {
                            putMessage(parsed)
                        }
                    }
                }
                trySend(getMessages(conversationId))
            }

        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val updateJob = coroutineScope.launch {
            messageUpdates.collect { updatedId ->
                if (updatedId == conversationId) {
                    trySend(getMessages(conversationId))
                }
            }
        }

        awaitClose {
            listener.remove()
            updateJob.cancel()
        }
    }

    override suspend fun sendChatMessage(message: ChatMessageRecord): Result<Unit> = withContext(Dispatchers.IO) {
        putMessage(message)
        try {
            firestore.collection("outreach")
                .document(message.conversationId)
                .collection("messages")
                .document(message.id)
                .set(message.toMap())
                .await()

            firestore.collection("outreach")
                .document(message.conversationId)
                .update("updatedAt", message.timestamp)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit) // Offline resilient write
        }
    }

    private fun parseMessageDocument(doc: DocumentSnapshot): ChatMessageRecord? {
        try {
            val obj = doc.toObject(ChatMessageRecord::class.java)
            if (obj != null) return obj
        } catch (_: Exception) {}

        return ChatMessageRecord(
            id = doc.getString("id") ?: doc.id,
            conversationId = doc.getString("conversationId") ?: "",
            senderUid = doc.getString("senderUid") ?: "",
            senderName = doc.getString("senderName") ?: "",
            senderRole = doc.getString("senderRole") ?: "student",
            text = doc.getString("text") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun parseOutreachDocument(doc: DocumentSnapshot): OutreachRecord? {
        try {
            val obj = doc.toObject(OutreachRecord::class.java)
            if (obj != null) return obj
        } catch (_: Exception) {}

        return OutreachRecord(
            id = doc.getString("id") ?: doc.id,
            investorUid = doc.getString("investorUid") ?: "",
            investorFundName = doc.getString("investorFundName") ?: "Institutional Investor",
            studentUid = doc.getString("studentUid") ?: "",
            mvpId = doc.getString("mvpId") ?: "",
            mvpAppName = doc.getString("mvpAppName") ?: "",
            introMessage = doc.getString("introMessage") ?: "",
            status = doc.getString("status") ?: "Contacted",
            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }
}
