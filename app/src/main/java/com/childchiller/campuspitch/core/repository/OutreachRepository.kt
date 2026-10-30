package com.childchiller.campuspitch.core.repository

import com.childchiller.campuspitch.core.models.ChatMessageRecord
import com.childchiller.campuspitch.core.models.OutreachRecord
import kotlinx.coroutines.flow.Flow

interface OutreachRepository {
    fun observeForStudent(studentUid: String): Flow<List<OutreachRecord>>
    fun observeForInvestor(investorUid: String): Flow<List<OutreachRecord>>
    suspend fun sendOutreach(outreach: OutreachRecord): Result<Unit>
    suspend fun updateOutreachStatus(outreachId: String, newStatus: String): Result<Unit>

    fun observeMessages(conversationId: String): Flow<List<ChatMessageRecord>>
    suspend fun sendChatMessage(message: ChatMessageRecord): Result<Unit>
}
