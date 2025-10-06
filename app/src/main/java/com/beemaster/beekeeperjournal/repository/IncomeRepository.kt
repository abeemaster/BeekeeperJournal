// IncomeRepository.kt
// Цей клас керуватиме доступом до даних прибутків.
// IncomeRepository.kt
// Цей клас керуватиме доступом до даних прибутків, працюючи з бізнес-моделлю Income.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.IncomeDao
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
// ✅ НОВІ ІМПОРТИ
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
    fun getAllIncomes(): Flow<List<Income>> { // 💡 Змінено на Income
        // ✅ КОНВЕРТАЦІЯ FLOW: Entity -> Model
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
    suspend fun insertIncome(income: Income) { // 💡 Змінено на Income
        // ✅ КОНВЕРТАЦІЯ: Model -> Entity перед записом
        incomeDao.insertIncome(income.toIncomeEntity())
    }

    /**
     * Оновлює існуючий прибуток у базі, конвертуючи Income у IncomeEntity.
     * @param income Бізнес-модель Income для оновлення.
     */
    suspend fun updateIncome(income: Income) { // 💡 Змінено на Income
        // ✅ КОНВЕРТАЦІЯ: Model -> Entity перед оновленням
        incomeDao.updateIncome(income.toIncomeEntity())
    }

    suspend fun deleteIncome(incomeId: Int) {
        incomeDao.deleteIncome(incomeId)
    }

    /**
     * Отримує прибуток за його ID. Конвертує IncomeEntity у Income.
     * @return Об'єкт Income або null.
     */
    suspend fun getIncomeById(incomeId: Int): Income? { // 💡 Змінено на Income?
        // ✅ КОНВЕРТАЦІЯ: Entity -> Model після читання
        return incomeDao.getIncomeById(incomeId)?.toIncome()
    }

    /**
     * Отримує всі прибутки (не Flow). Конвертує List<IncomeEntity> у List<Income>.
     * @return Список об'єктів Income.
     */
    suspend fun getAllIncomesSuspend(): List<Income> { // 💡 Змінено на List<Income>
        // ✅ КОНВЕРТАЦІЯ: Entity -> Model
        return incomeDao.getAllIncomesSuspend().map { it.toIncome() }
    }

    /**
     * Імпортує прибутки, конвертуючи список Income у список IncomeEntity.
     * @param incomes Список Income для імпорту.
     */
    suspend fun importIncomes(incomes: List<Income>) { // 💡 Змінено на List<Income>
        // ✅ КОНВЕРТАЦІЯ СПИСКУ: Model -> Entity
        val incomeEntities = incomes.map { it.toIncomeEntity() }
        incomeDao.clearAndInsertIncomes(incomeEntities)
    }

    suspend fun getFinalTotalIncome(): Double? {
        return incomeDao.getTotalIncomeSuspend()
    }

}