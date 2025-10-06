package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.models.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для роботи з сутностями [ExpenseEntity] у базі даних Room.
 */
@Dao
interface ExpenseDao {
    /**
     * Вставляє нову витрату в базу даних або замінює існуючу в разі конфлікту ID.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    /**
     * Оновлює інформацію про існуючу витрату.
     */
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    /**
     * Видаляє витрату з бази даних за об'єктом.
     */
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    /**
     * Видаляє витрату з бази даних за її унікальним ідентифікатором.
     */
    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    /**
     * Отримує всі витрати з бази даних.
     * Повертає [Flow], що емітує новий список щоразу, коли дані змінюються.
     */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    /**
     * Отримує витрату за її унікальним ідентифікатором.
     *
     * @return [ExpenseEntity] або null, якщо не знайдено.
     */
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Int): ExpenseEntity?

    /**
     * Отримує суму всіх витрат.
     * Повертає [Flow], що автоматично оновлюється при зміні даних.
     */
    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenseFlow(): Flow<Double?>

    /**
     * Отримує суму всіх витрат як одноразове значення (для синхронних операцій).
     */
    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalExpenseSuspend(): Double?

    /**
     * Отримує всі витрати як статичний список. Використовується, як правило, для операцій бекапу.
     */
    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesSuspend(): List<ExpenseEntity>

    /**
     * Вставляє список витрат у базу даних (для операцій імпорту/відновлення).
     * Використовує OnConflictStrategy.REPLACE для оновлення існуючих записів.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    /**
     * Очищає всю таблицю витрат. Використовується перед операціями імпорту.
     */
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

}