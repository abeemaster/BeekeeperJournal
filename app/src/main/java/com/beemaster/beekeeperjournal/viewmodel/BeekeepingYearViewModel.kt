// viewmodel/BeekeepingYearViewModel.kt

package com.beemaster.beekeeperjournal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.dao.BeekeepingYearDao
import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear
import com.beemaster.beekeeperjournal.utils.YearPrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class BeekeepingYearViewModel @Inject constructor(
    private val yearDao: BeekeepingYearDao,
    private val yearPrefsManager: YearPrefsManager,
    private val noteRepository: NoteRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) : ViewModel() {

    // Потік всіх років з БД
    private val allYearsFlow = yearDao.getAllYears()

    /**
     * StateFlow, що об'єднує список усіх років та ID активного року.
     * Це дозволяє UI реактивно відображати активний стан.
     */
    val yearListState: StateFlow<YearListState> = combine(
        allYearsFlow,
        yearPrefsManager.activeYearId // ID активного року з SharedPreferences
    ) { years, activeId ->
        YearListState(
            years = years,
            activeYearId = activeId
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = YearListState()
    )
    init {
        // Запускаємо перевірку при створенні ViewModel
        ensureActiveYearExists()
    }

    /**
     * Створює та вставляє новий пасічний рік (наприклад, для наступного сезону).
     */
    fun createNewYear(yearName: String, startDate: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val newYear = BeekeepingYear(name = yearName, startDate = startDate)
            val newId = yearDao.insertYear(newYear)
            // Одразу робимо його активним
            setActiveYear(newId)
        }
    }

    /**
     * Встановлює вибраний рік як активний у SharedPreferences.
     */
    fun setActiveYear(yearId: Long) {
        yearPrefsManager.setActiveYearId(yearId)
    }

    /**
     * Клас стану для UI
     */
    data class YearListState(
        val years: List<BeekeepingYear> = emptyList(),
        val activeYearId: Long = 1L
    )

    /**
     * Перевіряє наявність активного року. Якщо нічого не знайдено, створює поточний рік
     * та встановлює його як активний.
     */
    private fun ensureActiveYearExists() {
        viewModelScope.launch {
            // Отримуємо поточний активний ID з SharedPreferences
            val activeId = yearPrefsManager.activeYearId.first()

            // Перевіряємо, чи існує цей рік в базі даних.
            // Якщо activeId == 0L (значення за замовчуванням при першій установці) АБО
            // якщо рік за цим ID не знайдено в БД, потрібно створити новий.
            val currentActiveYear = yearDao.getYearById(activeId)

            if (activeId == 0L || currentActiveYear == null) {

                // --- ЛОГІКА СТВОРЕННЯ ПОТОЧНОГО РОКУ ---

                // 1. Отримуємо поточний рік.
                val calendar = Calendar.getInstance()
                val currentYearName = calendar.get(Calendar.YEAR).toString() // "2025"

                // 2. Створюємо об'єкт BeekeepingYear, використовуючи 1 січня поточного року як startDate.
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis

                // 3. Перевіряємо, чи вже існує в БД рік з такою назвою ("2025"),
                // щоб уникнути дублювання, якщо програма вже запускалась, але activeId був втрачений.
                val existingYear = yearDao.getYearByName(currentYearName)

                if (existingYear == null) {
                    // Якщо такого року немає, створюємо і робимо його активним
                    val newYear = BeekeepingYear(name = currentYearName, startDate = startDate)
                    val newId = yearDao.insertYear(newYear)

                    // Встановлюємо активний рік у SharedPreferences
                    yearPrefsManager.setActiveYearId(newId)
                    Log.d("BeekeepingYearViewModel", "Створено та встановлено активний рік: $currentYearName (ID: $newId)")
                } else {
                    // Якщо рік вже існує, просто встановлюємо його як активний.
                    yearPrefsManager.setActiveYearId(existingYear.yearId)
                    Log.d("BeekeepingYearViewModel", "Знайдено існуючий рік: $currentYearName (ID: ${existingYear.yearId}). Встановлено як активний.")
                }
            }
        }
    }

    /**
     * Додано функцію для оновлення назви існуючого року.
     * @param yearId ID року для оновлення.
     * @param newName Нова назва року.
     * @param startDate Дата початку року (передається без змін).
     * @return true, якщо оновлення успішне.
     */
    suspend fun updateYear(yearId: Long, newName: String, startDate: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Створюємо новий об'єкт BeekeepingYear з оновленою назвою,
                // використовуючи існуючий ID та стару дату початку.
                val updatedYear = BeekeepingYear(
                    yearId = yearId,
                    name = newName,
                    startDate = startDate
                )
                // Викликаємо оновлення через DAO
                yearDao.updateYear(updatedYear) //
                true // Успіх
            } catch (e: Exception) {
                Log.e("YearViewModel", "Помилка при оновленні року ID: $yearId", e)
                false // Помилка
            }
        }
    }

    /**
     * Видаляє пасічний рік за його ID та каскадно видаляє всі пов'язані дані.
     * @param yearId ID року для видалення.
     * @return true, якщо видалення успішне, false, якщо це був останній рік або сталася помилка.
     */
    suspend fun deleteYear(yearId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                performYearDeletion(yearId)
            } catch (e: Exception) {
                Log.e("YearViewModel", "Помилка при видаленні року ID: $yearId", e)
                false // Помилка видалення
            }
        }
    }

    /**
     * Внутрішня логіка видалення, виконується в IO-контексті.
     */
    private suspend fun performYearDeletion(yearId: Long): Boolean {
        // 1. ПЕРЕВІРКА: Отримуємо всі роки, щоб перевірити кількість
        val allYears = yearDao.getAllYears().first()

        if (allYears.size <= 1) {
            // Запобігаємо видаленню останнього року
            Log.w("YearViewModel", "Неможливо видалити останній рік (ID: $yearId).")
            return false
        }

        // 2. ПЕРЕКЛЮЧЕННЯ АКТИВНОГО РОКУ
        val isActiveYear = yearPrefsManager.activeYearId.first() == yearId
        if (isActiveYear) {
            val newActiveYearId = allYears
                .filter { it.yearId != yearId }
                .maxOfOrNull { it.yearId } ?: allYears.first().yearId

            if (newActiveYearId != yearId) {
                yearPrefsManager.setActiveYearId(newActiveYearId)
            }
        }

        // 3. КАСКАДНЕ ВИДАЛЕННЯ ДАНИХ
        noteRepository.deleteNotesByYearId(yearId)
        expenseRepository.deleteExpensesByYearId(yearId)
        incomeRepository.deleteIncomesByYearId(yearId)

        // 4. ВИДАЛЕННЯ САМОГО РОКУ
        yearDao.deleteYearById(yearId)

        return true
    }
}
