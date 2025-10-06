// ExpenseRepository.kt
// Цей клас керуватиме доступом до даних витрат, працюючи з бізнес-моделлю Expense.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.ExpenseDao
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.mappers.toExpense
import com.beemaster.beekeeperjournal.mappers.toExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    /**
     * Отримує потік усіх витрат. Конвертує List<ExpenseEntity> у List<Expense>.
     * @return Flow зі списком об'єктів Expense.
     */
    fun getAllExpenses(): Flow<List<Expense>> {
        // Використовуємо .map для конвертації Flow
        return expenseDao.getAllExpenses().map { entities ->
            entities.map { it.toExpense() }
        }
    }

    /**
     * Отримує потік загальної суми витрат. (Не вимагає конвертації).
     */
    fun getTotalExpenseFlow(): Flow<Double?> {
        return expenseDao.getTotalExpenseFlow()
    }

    /**
     * Вставляє нову витрату в базу, конвертуючи Expense у ExpenseEntity.
     * @param expense Бізнес-модель Expense для вставки.
     */
    suspend fun insertExpense(expense: Expense) {
        // Конвертуємо Model у Entity перед записом у DAO
        expenseDao.insertExpense(expense.toExpenseEntity())
    }

    /**
     * Оновлює існуючу витрату в базі, конвертуючи Expense у ExpenseEntity.
     * @param expense Бізнес-модель Expense для оновлення.
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
     * Отримує витрату за її ID. Конвертує ExpenseEntity у Expense.
     * @return Об'єкт Expense або null.
     */
    suspend fun getExpenseById(expenseId: Int): Expense? {
        // ✅ Конвертуємо Entity у Model після читання з DAO
        return expenseDao.getExpenseById(expenseId)?.toExpense()
    }

    /**
     * Отримує всі витрати (не Flow). Конвертує List<ExpenseEntity> у List<Expense>.
     * @return Список об'єктів Expense.
     */
    suspend fun getAllExpensesSuspend(): List<Expense> {
        // ✅ Конвертуємо Entity у Model
        return expenseDao.getAllExpensesSuspend().map { it.toExpense() }
    }

    /**
     * Імпортує витрати. Тут ми можемо залишити конвертацію Entity у Entity,
     * або змінити на List<Expense> і конвертувати. Залишаємо поточний тип
     * для спрощення імпорту з бекапу (де використовуються Entity).
     */
    suspend fun importExpenses(expenses: List<Expense>) {
        // ✅ ВИПРАВЛЕНО: Конвертуємо кожен елемент списку за допомогою map,
        // щоб передати DAO коректний List<ExpenseEntity>.
        val expenseEntities = expenses.map { it.toExpenseEntity() }
        expenseDao.insertExpenses(expenseEntities)
    }
}