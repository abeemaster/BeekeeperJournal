package com.beemaster.beekeeperjournal.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    // ✅ Додаємо метод для видалення за ID
    @Query("DELETE FROM incomes WHERE id = :incomeId")
    suspend fun deleteIncome(incomeId: Int)

    // ✅ Додаємо метод для отримання загальної суми прибутків
    @Query("SELECT SUM(totalAmount) FROM incomes")
    fun getTotalIncomeFlow(): Flow<Double?>

    // ✅ Додаємо метод для отримання прибутку за ID
    @Query("SELECT * FROM incomes WHERE id = :incomeId")
    suspend fun getIncomeById(incomeId: Int): IncomeEntity?

}