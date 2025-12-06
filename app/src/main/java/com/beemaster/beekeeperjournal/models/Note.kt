// Note.kt

package com.beemaster.beekeeperjournal.models

/**
 * Клас даних, що представляє один запис.
 * Це чиста бізнес-модель (Domain Model), незалежна від бази даних.
 *
 * @param id Унікальний ідентифікатор запису.
 * @param text Зміст запису.
 * @param type Тип запису (наприклад, "hive", "general", "queen", "notes").
 * @param hiveNumber ID вулика, до якого відноситься запис (0 для загальних записів).
 * @param timestamp Мітка часу створення запису (для сортування).
 * @param title Заголовок нотатки, який відображається у списках.
 */
data class Note(
    val id: Int,
    val text: String,
    val type: String,
    val hiveId: Int,
    val timestamp: Long,
    val yearId: Int = 0,
    val title: String,
    val imagePath: String? = null // Шлях до зображення
)
