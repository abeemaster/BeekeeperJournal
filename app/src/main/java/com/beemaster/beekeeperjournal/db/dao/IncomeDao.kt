package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для роботи з сутностями [IncomeEntity] (прибутки) у базі даних Room.
 */
@Dao
interface IncomeDao {

    // --- CRUD Operations (Без змін) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :incomeId")
    suspend fun deleteIncome(incomeId: Int)

    // --- Оновлені методи з Фільтрацією за Роком ---

    /**
     * Отримує всі прибутки з бази даних.
     * @param activeYearId ID поточного активного пасічного року.
     * @return Flow зі списком об'єктів [IncomeEntity], відфільтрованих за роком.
     */
    @Query("SELECT * FROM incomes WHERE yearId = :activeYearId ORDER BY date DESC")
    fun getAllIncomes(activeYearId: Int): Flow<List<IncomeEntity>>

    /**
     * Отримує потік загальної суми прибутків, фільтруючи за активним роком.
     * @param activeYearId ID поточного активного пасічного року.
     * @return Flow, що містить загальну суму прибутків ([Double] або null).
     */
    @Query("SELECT SUM(totalAmount) FROM incomes WHERE yearId = :activeYearId")
    fun getTotalIncomeFlow(activeYearId: Int): Flow<Double?>

    // --- Додаткові синхронні методи (Без змін у фільтрації) ---

    @Query("SELECT * FROM incomes WHERE id = :incomeId")
    suspend fun getIncomeById(incomeId: Int): IncomeEntity?

    @Query("SELECT SUM(totalAmount) FROM incomes")
    suspend fun getTotalIncomeSuspend(): Double?

    @Query("SELECT * FROM incomes")
    suspend fun getAllIncomesSuspend(): List<IncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<IncomeEntity>)

    @Query("DELETE FROM incomes")
    suspend fun deleteAllIncomes()

    @Transaction
    suspend fun clearAndInsertIncomes(incomes: List<IncomeEntity>) {
        deleteAllIncomes()
        insertIncomes(incomes)
    }
}