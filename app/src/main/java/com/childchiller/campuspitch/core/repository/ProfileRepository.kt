package com.childchiller.campuspitch.core.repository

import com.childchiller.campuspitch.core.models.AppUserRecord
import com.childchiller.campuspitch.core.models.InvestorProfileRecord
import com.childchiller.campuspitch.core.models.StudentProfileRecord
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeUser(uid: String): Flow<AppUserRecord?>
    fun observeStudentProfile(uid: String): Flow<StudentProfileRecord?>
    fun observeInvestorProfile(uid: String): Flow<InvestorProfileRecord?>
    fun observePendingInvestors(): Flow<List<InvestorProfileRecord>>
    suspend fun saveUser(user: AppUserRecord): Result<Unit>
    suspend fun saveStudentProfile(profile: StudentProfileRecord): Result<Unit>
    suspend fun saveInvestorProfile(profile: InvestorProfileRecord): Result<Unit>
    suspend fun approveInvestor(investorUid: String, investorCode: String): Result<Unit>
    suspend fun rejectInvestor(investorUid: String): Result<Unit>
    suspend fun getInvestorCodeDoc(code: String): Result<InvestorProfileRecord?>
}
