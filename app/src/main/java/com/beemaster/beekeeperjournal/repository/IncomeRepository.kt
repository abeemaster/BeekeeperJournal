// IncomeRepository.kt
// Цей клас керуватиме доступом до даних прибутків.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.IncomeDao
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao
) {

    fun getAllIncomes(): Flow<List<IncomeEntity>> {
        return incomeDao.getAllIncomes()
    }

    fun getTotalIncomeFlow(): Flow<Double?> {
        return incomeDao.getTotalIncomeFlow()
    }

    suspend fun insertIncome(income: IncomeEntity) {
        incomeDao.insertIncome(income)
    }

    suspend fun updateIncome(income: IncomeEntity) {
        incomeDao.updateIncome(income)
    }

    suspend fun deleteIncome(incomeId: Int) {
        incomeDao.deleteIncome(incomeId)
    }

    suspend fun getIncomeById(incomeId: Int): IncomeEntity? {
        return incomeDao.getIncomeById(incomeId)
    }

    suspend fun getAllIncomesSuspend(): List<IncomeEntity> {
        return incomeDao.getAllIncomesSuspend()
    }
    suspend fun importIncomes(incomes: List<IncomeEntity>) {
        return incomeDao.clearAndInsertIncomes(incomes)
    }
    suspend fun getFinalTotalIncome(): Double? {
        return incomeDao.getTotalIncomeSuspend()
    }

}
