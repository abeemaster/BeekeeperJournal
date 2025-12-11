package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для роботи з сутностями [ExpenseEntity] у базі даних Room.
 */
@Dao
interface ExpenseDao {

    // --- CRUD Operations (Без змін) ---
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    /**
     * Видаляє всі витрати, пов'язані з певним пасічним роком.
     * @param yearId ID року для видалення.
     */
    @Query("DELETE FROM expenses WHERE yearId = :yearId")
    suspend fun deleteExpensesByYearId(yearId: Long) // 👈 ДОДАЙТЕ ЦЕ

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    // --- Оновлені методи з Фільтрацією за Роком ---

    /**
     * Отримує всі витрати з бази даних.
     * @param activeYearId ID поточного активного пасічного року.
     * @return Flow зі списком об'єктів [ExpenseEntity], відфільтрованих за роком.
     */
    @Query("SELECT * FROM expenses WHERE yearId = :activeYearId ORDER BY date DESC")
    fun getAllExpenses(activeYearId: Int): Flow<List<ExpenseEntity>>

    /**
     * Отримує потік загальної суми витрат, фільтруючи за активним роком.
     * @param activeYearId ID поточного активного пасічного року.
     * @return Flow, що містить загальну суму витрат ([Double] або null).
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE yearId = :activeYearId")
    fun getTotalExpenseFlow(activeYearId: Int): Flow<Double?>

    // --- Додаткові синхронні методи (Без змін у фільтрації) ---

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Int): ExpenseEntity?

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalExpenseSuspend(): Double?

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesSuspend(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Update
    suspend fun updateExpenses(expenses: List<ExpenseEntity>)

    // ... (інші методи)
}
//-------------------------------

