package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.BeekeepingYearDao
import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YearRepository @Inject constructor(
    private val yearDao: BeekeepingYearDao
) {
    /**
     * Отримує всі роки як Flow для спостереження в UI.
     */
    fun getAllYears(): Flow<List<BeekeepingYear>> = yearDao.getAllYears()

    /**
     * Отримує список всіх років без Flow (для експорту/бекапу).
     */
    suspend fun getAllYearsStatic(): List<BeekeepingYear> = withContext(Dispatchers.IO) {
        yearDao.getAllYearsStatic()
    }

    /**
     * Очищує таблицю та вставляє нові роки (для імпорту).
     */
    suspend fun importYears(years: List<BeekeepingYear>) = withContext(Dispatchers.IO) {
        yearDao.clearAndInsertYears(years)
    }

    /**
     * Додає або оновлює один рік.
     */
    suspend fun upsertYear(year: BeekeepingYear) = withContext(Dispatchers.IO) {
        yearDao.insertYear(year)
    }
}