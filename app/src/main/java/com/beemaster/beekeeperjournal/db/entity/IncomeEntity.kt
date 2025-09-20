package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.beemaster.beekeeperjournal.db.Converters
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
    val productName: String, // (назва продукції)
    val quantity: Double, // (кількість продукції)
    val price: Double, // (ціна за одиницю)
    val totalAmount: Double // (загальна сума, отримана з продажу)
)