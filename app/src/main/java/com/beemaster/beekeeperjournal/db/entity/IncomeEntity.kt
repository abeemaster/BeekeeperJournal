package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Клас-сутність для збереження інформації про прибутки.
 * Використовує Long (Unix timestamp) для збереження дати.
 */
@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    /** ID вулика, до якого належить цей прибуток. */
    val hiveId: Int,
    /** Дата прибутку у форматі Unix timestamp (мілісекунди). */
    val date: Long,
    /** Назва проданої продукції (наприклад, "Мед травневий"). */
    val productName: String,
    /** Кількість проданої продукції. */
    val quantity: Double,
    /** Одиниця виміру (наприклад, "кг", "л", "шт"). */
    val unitName: String,
    /** Ціна за одну одиницю продукції. */
    val price: Double,
    /** Загальна сума, отримана з продажу (quantity * price). */
    val totalAmount: Double
)