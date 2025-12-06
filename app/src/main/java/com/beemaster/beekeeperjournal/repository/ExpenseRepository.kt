// ExpenseRepository.kt
// Цей клас керуватиме доступом до даних витрат, працюючи з бізнес-моделлю Expense.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.ExpenseDao
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.mappers.toExpense
import com.beemaster.beekeeperjournal.mappers.toExpenseEntity
import com.beemaster.beekeeperjournal.utils.YearPrefsManager // НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest // НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.first // НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val yearPrefsManager: YearPrefsManager // ІНЖЕКЦІЯ
) {

    /**
     * Отримує потік усіх витрат, фільтруючи за активним роком.
     * @return Flow зі списком об'єктів Expense.
     */
    fun getAllExpenses(): Flow<List<Expense>> {
        // Комбінуємо потік ID року з потоком даних DAO
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                val yearId = yearIdLong.toInt()
                // !!! ПОТРЕБУЄ ExpenseDao.getAllExpenses(activeYearId: Int) !!!
                expenseDao.getAllExpenses(yearId)
            }
            .map { entities ->
                entities.map { it.toExpense() }
            }
    }

    /**
     * Отримує потік загальної суми витрат, фільтруючи за активним роком.
     */
    fun getTotalExpenseFlow(): Flow<Double?> {
        // Комбінуємо потік ID року з потоком суми DAO
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                val yearId = yearIdLong.toInt()
                // !!! ПОТРЕБУЄ ExpenseDao.getTotalExpenseFlow(activeYearId: Int) !!!
                expenseDao.getTotalExpenseFlow(yearId)
            }
    }

    /**
     * Вставляє нову витрату в базу, присвоюючи активний yearId, якщо він не встановлений.
     */
    suspend fun insertExpense(expense: Expense) {
        val expenseWithYearId = if (expense.yearId == 0) {
            val currentYearId = yearPrefsManager.activeYearId.first().toInt()
            expense.copy(yearId = currentYearId)
        } else {
            expense
        }
        // Конвертуємо Model у Entity перед записом у DAO
        expenseDao.insertExpense(expenseWithYearId.toExpenseEntity())
    }

    /**
     * Оновлює існуючу витрату в базі.
     */
    suspend fun updateExpense(expense: Expense) {
        // Конвертуємо Model у Entity перед оновленням у DAO
        expenseDao.updateExpense(expense.toExpenseEntity())
    }

    /**
     * Видаляє витрату за її ID. (Не вимагає конвертації).
     */
    suspend fun deleteExpense(expenseId: Int) {
        expenseDao.deleteExpense(expenseId)
    }

    /**
     * Отримує всі витрати (не Flow). Конвертує List<ExpenseEntity> у List<Expense>.
     * @return Список об'єктів Expense.
     */
    suspend fun getAllExpensesSuspend(): List<Expense> {
        //  Конвертуємо Entity у Model
        return expenseDao.getAllExpensesSuspend().map { it.toExpense() }
    }

    /**
     * Імпортує витрати. Тут ми можемо залишити конвертацію Entity у Entity,
     * або змінити на List<Expense> і конвертувати. Залишаємо поточний тип
     * для спрощення імпорту з бекапу (де використовуються Entity).
     */
    suspend fun importExpenses(expenses: List<Expense>) {
        // Конвертуємо кожен елемент списку за допомогою map,
        // щоб передати DAO коректний List<ExpenseEntity>.
        val expenseEntities = expenses.map { it.toExpenseEntity() }
        expenseDao.insertExpenses(expenseEntities)
    }
}