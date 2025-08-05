// Note.kt

package com.beemaster.beekeeperjournal

/**
 * Клас даних, що представляє один запис.
 * Більше не є сутністю Room.
 *
 * @param id Унікальний ідентифікатор запису.
 * @param date Дата створення або останнього редагування запису (наприклад, "01-07-24").
 * @param text Зміст запису.
 * @param type Тип запису (наприклад, "hive", "general", "queen", "notes").
 * @param hiveNumber Номер вулика, до якого відноситься запис (0 для загальних записів).
 * @param timestamp Мітка часу створення запису (для сортування).
 */
data class Note(
    val id: String, // Змінено на String для UUID, оскільки Room більше не генерує Long ID
    val date: String,
    val text: String,
    val type: String,
    val hiveNumber: Int,
    val timestamp: Long
)
