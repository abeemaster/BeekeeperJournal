package com.beemaster.beekeeperjournal.db.dao

import androidx.room.*
import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для керування сутністю BeekeepingYear.
 */
@Dao
interface BeekeepingYearDao {

    /**
     * Повертає всі пасічні роки, відсортовані за датою початку.
     */
    @Query("SELECT * FROM beekeeping_years ORDER BY startDate DESC")
    fun getAllYears(): Flow<List<BeekeepingYear>>

    /**
     * Вставляє новий пасічний рік.
     * @param year Об'єкт BeekeepingYear для вставки.
     * @return ID щойно вставленого рядка.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYear(year: BeekeepingYear): Long

    /**
     * Оновлює існуючий пасічний рік.
     */
    @Update
    suspend fun updateYear(year: BeekeepingYear)

    /**
     * Видаляє пасічний рік за його ID.
     */
    @Query("DELETE FROM beekeeping_years WHERE yearId = :yearId")
    suspend fun deleteYearById(yearId: Long)

    /**
     * Отримує рік за його унікальною назвою (наприклад, "2025").
     */
    @Query("SELECT * FROM beekeeping_years WHERE name = :yearName")
    suspend fun getYearByName(yearName: String): BeekeepingYear? // <-- ДОДАЄМО ЦЕЙ МЕТОД

    /**
     * Отримує рік за його ID.
     */
    @Query("SELECT * FROM beekeeping_years WHERE yearId = :yearId")
    suspend fun getYearById(yearId: Long): BeekeepingYear?
}