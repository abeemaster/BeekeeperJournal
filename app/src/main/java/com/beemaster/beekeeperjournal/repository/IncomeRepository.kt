// IncomeRepository.kt
// Цей клас керуватиме доступом до даних прибутків, працюючи з бізнес-моделлю Income.


package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.IncomeDao
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.mappers.toIncome
import com.beemaster.beekeeperjournal.mappers.toIncomeEntity
import com.beemaster.beekeeperjournal.utils.YearPrefsManager // НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest // НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.first // НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
    private val yearPrefsManager: YearPrefsManager // ІНЖЕКЦІЯ
) {

    /**
     * Отримує потік усіх прибутків, фільтруючи за активним роком.
     * @return Flow зі списком об'єктів Income.
     */
    fun getAllIncomes(): Flow<List<Income>> {
        // Комбінуємо потік ID року з потоком даних DAO
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                val yearId = yearIdLong.toInt()
                // !!! ПОТРЕБУЄ IncomeDao.getAllIncomes(activeYearId: Int) !!!
                incomeDao.getAllIncomes(yearId)
            }
            .map { entities ->
                entities.map { it.toIncome() }
            }
    }

    /**
     * Отримує потік загальної суми прибутків, фільтруючи за активним роком.
     */
    fun getTotalIncomeFlow(): Flow<Double?> {
        // Комбінуємо потік ID року з потоком суми DAO
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                val yearId = yearIdLong.toInt()
                // !!! ПОТРЕБУЄ IncomeDao.getTotalIncomeFlow(activeYearId: Int) !!!
                incomeDao.getTotalIncomeFlow(yearId)
            }
    }

    /**
     * Вставляє новий прибуток у базу, присвоюючи активний yearId, якщо він не встановлений.
     */
    suspend fun insertIncome(income: Income) {
        val incomeWithYearId = if (income.yearId == 0) {
            val currentYearId = yearPrefsManager.activeYearId.first().toInt()
            income.copy(yearId = currentYearId)
        } else {
            income
        }
        incomeDao.insertIncome(incomeWithYearId.toIncomeEntity())
    }

    /**
     * Оновлює існуючий прибуток у базі.
     */
    suspend fun updateIncome(income: Income) {
        incomeDao.updateIncome(income.toIncomeEntity())
    }

    suspend fun deleteIncome(incomeId: Int) {
        incomeDao.deleteIncome(incomeId)
    }

    /**
     * Отримує всі прибутки як статичний список (для бекапу). Фільтрація не потрібна.
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