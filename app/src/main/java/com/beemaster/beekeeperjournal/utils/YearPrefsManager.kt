// utils/YearPrefsManager.kt

package com.beemaster.beekeeperjournal.utils

import android.content.Context
import com.beemaster.beekeeperjournal.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YearPrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

    // Використовуємо StateFlow для реактивного доступу до активного ID року
    private val _activeYearId = MutableStateFlow(loadActiveYearId())
    val activeYearId: StateFlow<Long> = _activeYearId

    init {
        // Якщо при першому запуску ID не встановлено (наприклад, 0), встановлюємо його на 1 (наш початковий рік).
        if (loadActiveYearId() == 0L) {
            setActiveYearId(1L)
        }
    }

    private fun loadActiveYearId(): Long {
        // Повертаємо збережений ID, або 1L як значення за замовчуванням (ID початкового року)
        return prefs.getLong(Constants.KEY_ACTIVE_YEAR_ID, 1L)
    }

    /**
     * Зберігає новий активний ID року у SharedPreferences та оновлює StateFlow.
     */
    fun setActiveYearId(yearId: Long) {
        prefs.edit().putLong(Constants.KEY_ACTIVE_YEAR_ID, yearId).apply()
        _activeYearId.value = yearId
    }
}