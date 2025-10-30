package com.beemaster.beekeeperjournal.data

import kotlin.reflect.KClass

data class SettingItem(
    val title: String,
    val targetActivity: Class<*> // Клас, який потрібно запустити
)