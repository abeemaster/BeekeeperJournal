// IncomeRepository.kt
// Цей клас керуватиме доступом до даних прибутків.
// IncomeRepository.kt
// Цей клас керуватиме доступом до даних прибутків, працюючи з бізнес-моделлю Income.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.IncomeDao
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.mappers.toIncome
import com.beemaster.beekeeperjournal.mappers.toIncomeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map // ✅ ДЛЯ КОНВЕРТАЦІЇ FLOW
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao
) {

    /**
     * Отримує потік усіх прибутків. Конвертує List<IncomeEntity> у List<Income>.
     * @return Flow зі списком об'єктів Income.
     */
    fun getAllIncomes(): Flow<List<Income>> {
        return incomeDao.getAllIncomes().map { entities ->
            entities.map { it.toIncome() }
        }
    }

    /**
     * Отримує потік загальної суми прибутків. (Не вимагає конвертації).
     */
    fun getTotalIncomeFlow(): Flow<Double?> {
        return incomeDao.getTotalIncomeFlow()
    }

    /**
     * Вставляє новий прибуток у базу, конвертуючи Income у IncomeEntity.
     * @param income Бізнес-модель Income для вставки.
     */
    suspend fun insertIncome(income: Income) {
        incomeDao.insertIncome(income.toIncomeEntity())
    }

    /**
     * Оновлює існуючий прибуток у базі, конвертуючи Income у IncomeEntity.
     * @param income Бізнес-модель Income для оновлення.
     */
    suspend fun updateIncome(income: Income) {
        incomeDao.updateIncome(income.toIncomeEntity())
    }

    suspend fun deleteIncome(incomeId: Int) {
        incomeDao.deleteIncome(incomeId)
    }


    /**
     * Отримує всі прибутки (не Flow). Конвертує List<IncomeEntity> у List<Income>.
     * @return Список об'єктів Income.
     */
    suspend fun getAllIncomesSuspend(): List<Income> {
        return incomeDao.getAllIncomesSuspend().map { it.toIncome() }
    }

    /**
     * Імпортує прибутки, конвертуючи список Income у список IncomeEntity.
     * @param incomes Список Income для імпорту.
     */
    suspend fun importIncomes(incomes: List<Income>) {
        val incomeEntities = incomes.map { it.toIncomeEntity() }
        incomeDao.clearAndInsertIncomes(incomeEntities)
    }

}