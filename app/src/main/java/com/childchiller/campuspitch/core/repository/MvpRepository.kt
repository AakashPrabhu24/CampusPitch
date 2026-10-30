package com.childchiller.campuspitch.core.repository

import com.childchiller.campuspitch.core.models.MvpPitchRecord
import kotlinx.coroutines.flow.Flow

/**
 * State representing a live write operation with pending-write awareness.
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String = "Saved") : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Contract for MVP data access and live observation.
 */
interface MvpRepository {
    fun observePublishedMvps(): Flow<List<MvpPitchRecord>>
    fun observeStudentMvps(studentUid: String): Flow<List<MvpPitchRecord>>
    suspend fun saveMvp(mvp: MvpPitchRecord): Result<Unit>
    suspend fun updateMvp(mvp: MvpPitchRecord): Result<Unit>
    suspend fun deleteMvp(mvpId: String): Result<Unit>
}
