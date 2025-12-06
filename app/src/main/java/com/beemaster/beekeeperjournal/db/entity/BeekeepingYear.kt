package com.beemaster.beekeeperjournal.db.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність, що представляє один пасічний рік (сезон).
 * Дозволяє розділяти всі записи, пов'язані з вуликами, за роками.
 */
@Entity(tableName = "beekeeping_years")
data class BeekeepingYear(
    @PrimaryKey(autoGenerate = true)
    val yearId: Long = 0,
    // Наприклад: "Сезон 2024" або "Пасічний рік 2025"
    val name: String,
    // Дата початку року, корисно для сортування та відображення
    val startDate: Long,

) {
    // Конструктор для зручного створення нового року, наприклад, від поточного
    constructor(name: String, startDate: Long) : this(0, name, startDate)
}