package com.beemaster.beekeeperjournal.viewmodel


import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.models.Note
import kotlinx.coroutines.Job

/**
 * Інтерфейс для забезпечення доступу до даних та логіки імпорту/експорту
 * для BackupManager, відокремлюючи його від конкретної реалізації ViewModel.
 */
interface BackupDataSource {
    // Методи для ЕКСПОРТУ
    suspend fun getAllHivesSuspend(): List<HiveEntity>
    suspend fun getAllNotesSuspend(): List<Note>
    suspend fun getAllExpensesSuspend(): List<Expense>
    suspend fun getAllIncomesSuspend(): List<Income>

    // Методи для ІМПОРТУ
    suspend fun importHives(hives: List<HiveEntity>)
    fun importNotes(notes: List<Note>): Job
    fun importExpenses(expenses: List<Expense>): Job
    fun importIncomes(incomes: List<Income>): Job
}