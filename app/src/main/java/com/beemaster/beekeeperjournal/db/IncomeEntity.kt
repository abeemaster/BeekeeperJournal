package com.beemaster.beekeeperjournal.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

/**
 * Клас-сутність для збереження інформації про прибутки.
 */
@Entity(tableName = "incomes")
@TypeConverters(Converters::class) // Використовуємо існуючий конвертер для дати
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Date,
    val productName: String,
    val quantity: Double,
    val price: Double,
    val totalAmount: Double
)