// viewmodel/BeekeepingYearViewModel.kt

package com.beemaster.beekeeperjournal.viewmodel

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
import javax.inject.Inject

@HiltViewModel
class BeekeepingYearViewModel @Inject constructor(
    private val yearDao: BeekeepingYearDao,
    private val yearPrefsManager: YearPrefsManager
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
}