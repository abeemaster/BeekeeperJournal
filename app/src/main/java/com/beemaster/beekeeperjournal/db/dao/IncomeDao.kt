package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
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

    // ✅ Додано: метод для отримання всіх прибутків для бекапу
    @Query("SELECT * FROM incomes")
    suspend fun getAllIncomesSuspend(): List<IncomeEntity>

    // ✅ Додано: метод для імпорту списку прибутків
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<IncomeEntity>)

    // ✅ Додано: метод для очищення та імпорту
    @Query("DELETE FROM incomes")
    suspend fun deleteAllIncomes()
}