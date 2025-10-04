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

    /**
     * Вставляє новий прибуток у базу даних або замінює існуючий в разі конфлікту ID.
     * @param income Об'єкт [IncomeEntity] для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    /**
     * Оновлює інформацію про існуючий прибуток.
     * @param income Об'єкт [IncomeEntity] для оновлення.
     */
    @Update
    suspend fun updateIncome(income: IncomeEntity)

    /**
     * Видаляє прибуток з бази даних за об'єктом.
     * @param income Об'єкт [IncomeEntity] для видалення.
     */
    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    /**
     * Отримує всі прибутки з бази даних, відсортовані за датою (від новіших до старіших).
     * Повертає [Flow], що емітує новий список при зміні даних.
     * @return [Flow], що містить список [IncomeEntity].
     */
    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    /**
     * Видаляє прибуток з бази даних за його унікальним ідентифікатором.
     * @param incomeId ID прибутку для видалення.
     */
    @Query("DELETE FROM incomes WHERE id = :incomeId")
    suspend fun deleteIncome(incomeId: Int)

    /**
     * Отримує суму всіх прибутків.
     * Повертає [Flow], що автоматично оновлюється при зміні даних (для відображення на UI).
     * @return [Flow], що містить загальну суму прибутків ([Double] або null).
     */
    @Query("SELECT SUM(totalAmount) FROM incomes")
    fun getTotalIncomeFlow(): Flow<Double?>

    /**
     * Отримує прибуток за його унікальним ідентифікатором.
     * @param incomeId ID прибутку.
     * @return [IncomeEntity] або null, якщо не знайдено.
     */
    @Query("SELECT * FROM incomes WHERE id = :incomeId")
    suspend fun getIncomeById(incomeId: Int): IncomeEntity?

    /**
     * Отримує суму всіх прибутків як одноразове значення (для синхронних операцій, наприклад, розрахунків).
     * @return Загальна сума прибутків ([Double] або null).
     */
    @Query("SELECT SUM(totalAmount) FROM incomes")
    suspend fun getTotalIncomeSuspend(): Double?

    /**
     * Отримує всі прибутки як статичний список. Використовується, як правило, для операцій бекапу.
     * @return Список усіх [IncomeEntity].
     */
    @Query("SELECT * FROM incomes")
    suspend fun getAllIncomesSuspend(): List<IncomeEntity>

    /**
     * Вставляє список прибутків у базу даних (для операцій імпорту/відновлення).
     * @param incomes Список [IncomeEntity] для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<IncomeEntity>)

    /**
     * Очищає всю таблицю прибутків. Використовується перед операціями імпорту.
     */
    @Query("DELETE FROM incomes")
    suspend fun deleteAllIncomes()

    /**
     * Виконує очищення таблиці прибутків та подальшу вставку нового списку
     * в рамках однієї атомарної транзакції (для імпорту/відновлення).
     * @param incomes Список [IncomeEntity] для імпорту.
     */
    @Transaction
    suspend fun clearAndInsertIncomes(incomes: List<IncomeEntity>) {
        deleteAllIncomes()
        insertIncomes(incomes)
    }
}