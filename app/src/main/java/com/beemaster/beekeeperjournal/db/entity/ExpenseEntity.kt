package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Клас-сутність для збереження інформації про витрати.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    /** Унікальний ідентифікатор витрати. Генерується автоматично. */
    val id: Int = 0,
    /** ID вулика, до якого належить ця витрата. */
    val hiveId: Int,
    /** Дата здійснення витрати у форматі Unix timestamp (мілісекунди). */
    val date: Long,
    /** Назва витрати (наприклад, "Цукровий сироп", "Ліки від кліща"). */
    val name: String,
    /** Кількість придбаної продукції/речовини. */
    val quantityUnits: Double,
    /** Одиниця виміру (наприклад, "кг", "л", "шт"). */
    val nameQuantity: String,
    /** Загальна сума витрати (вартість). */
    val amount: Double,
    /** Рік, до якого належить нотатка (наприклад, 2025). Використовується для фільтрації. */
    val yearId: Int
)