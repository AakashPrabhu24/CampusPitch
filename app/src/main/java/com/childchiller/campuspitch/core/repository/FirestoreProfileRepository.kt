package com.childchiller.campuspitch.core.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.childchiller.campuspitch.core.models.AppUserRecord
import com.childchiller.campuspitch.core.models.InvestorProfileRecord
import com.childchiller.campuspitch.core.models.StudentProfileRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProfileRepository {

    override fun observeUser(uid: String): Flow<AppUserRecord?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(AppUserRecord::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun observeStudentProfile(uid: String): Flow<StudentProfileRecord?> = callbackFlow {
        val listener = firestore.collection("students").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(StudentProfileRecord::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun observeInvestorProfile(uid: String): Flow<InvestorProfileRecord?> = callbackFlow {
        val listener = firestore.collection("investors").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(InvestorProfileRecord::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun observePendingInvestors(): Flow<List<InvestorProfileRecord>> = callbackFlow {
        val listener = firestore.collection("investors")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InvestorProfileRecord::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveUser(user: AppUserRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(user.uid).set(user.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveStudentProfile(profile: StudentProfileRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("students").document(profile.uid).set(profile.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveInvestorProfile(profile: InvestorProfileRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("investors").document(profile.uid).set(profile.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveInvestor(investorUid: String, investorCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Update investor status to verified and record issued code
            firestore.collection("investors").document(investorUid).update(
                mapOf(
                    "status" to "verified",
                    "issuedInvestorCode" to investorCode
                )
            ).await()

            // Fetch investor details to store in investor_codes directory
            val investorDoc = firestore.collection("investors").document(investorUid).get().await()
            val fundName = investorDoc.getString("fundName") ?: "Approved Fund"
            val officialEmail = investorDoc.getString("officialEmail") ?: ""

            firestore.collection("investor_codes").document(investorCode).set(
                mapOf(
                    "code" to investorCode,
                    "investorUid" to investorUid,
                    "fundName" to fundName,
                    "officialEmail" to officialEmail,
                    "verifiedAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectInvestor(investorUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("investors").document(investorUid).update("status", "rejected").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInvestorCodeDoc(code: String): Result<InvestorProfileRecord?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("investor_codes").document(code).get().await()
            if (doc.exists()) {
                val uid = doc.getString("investorUid") ?: ""
                val fund = doc.getString("fundName") ?: ""
                val email = doc.getString("officialEmail") ?: ""
                Result.success(
                    InvestorProfileRecord(
                        uid = uid,
                        fundName = fund,
                        officialEmail = email,
                        status = "verified",
                        issuedInvestorCode = code
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
