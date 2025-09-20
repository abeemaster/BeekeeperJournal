package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Int): ExpenseEntity?

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenseFlow(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalExpenseSuspend(): Double?

    // ✅ Додано: метод для отримання всіх витрат для бекапу
    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesSuspend(): List<ExpenseEntity>

    // ✅ Додано: метод для імпорту списку витрат
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    // ✅ Додано: метод для очищення та імпорту
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

}