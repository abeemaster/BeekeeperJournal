package com.beemaster.beekeeperjournal.data

data class SettingItem(
    val title: String,
    val targetActivity: Class<*> // Клас, який потрібно запустити
)

/**
 * Файл SettingItem.kt використовується для створення моделі даних, яка представляє один елемент (рядок)
 * у списку налаштувань вашого застосунку (наприклад, у SettingsActivity або SettingsFragment).
 */