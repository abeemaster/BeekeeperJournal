// ExpenseRepository.kt
// Цей клас керуватиме доступом до даних витрат.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.ExpenseDao
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    fun getAllExpenses(): Flow<List<ExpenseEntity>> {
        return expenseDao.getAllExpenses()
    }

    fun getTotalExpenseFlow(): Flow<Double?> {
        return expenseDao.getTotalExpenseFlow()
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expenseId: Int) {
        expenseDao.deleteExpense(expenseId)
    }

    suspend fun getExpenseById(expenseId: Int): ExpenseEntity? {
        return expenseDao.getExpenseById(expenseId)
    }

    suspend fun getAllExpensesSuspend(): List<ExpenseEntity> {
        return expenseDao.getAllExpensesSuspend()
    }

    suspend fun importExpenses(expenses: List<ExpenseEntity>) {
        return expenseDao.insertExpenses(expenses)
    }
}
