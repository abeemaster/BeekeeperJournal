package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.beemaster.beekeeperjournal.db.Converters
import java.util.Date

/**
 * Клас-су1тність для збереження інформації про витрати.
 */
@Entity(tableName = "expenses")
@TypeConverters(Converters::class) // Використовуємо існуючий конвертер для дати
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Date,
    val name: String, // (назва витрати)
    // val quantity: Double, // (кількість продукції)
    val amount: Double // (загальна сума)
)