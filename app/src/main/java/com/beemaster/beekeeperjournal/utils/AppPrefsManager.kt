// Цей клас буде відповідати за загальний стан додатка.

package com.beemaster.beekeeperjournal.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /**
     * Перевіряє, чи це перший запуск додатка.
     */
    fun isFirstRun(): Boolean = prefs.getBoolean("is_first_run", true)

    /**
     * Позначає, що початкове налаштування завершено.
     */
    fun setFirstRunCompleted() {
        prefs.edit().putBoolean("is_first_run", false).apply()
    }
}