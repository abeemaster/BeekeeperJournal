// Expense.kt

package com.beemaster.beekeeperjournal.models

import java.io.Serializable

/**
 * Клас даних, що представляє бізнес-модель однієї витрати.
 * Ця модель використовується у шарі Domain та Presentation (ViewModel/UI).
 * Вона незалежна від деталей реалізації бази даних (ExpenseEntity).
 *
 * @param id Унікальний ідентифікатор витрати (0 для нових записів).
 * @param hiveId ID вулика, до якого відноситься витрата (0 для загальних витрат).
 * @param date Мітка часу здійснення витрати (Unix timestamp, мілісекунди).
 * @param name Назва витрати (наприклад, "Цукровий сироп").
 * @param quantityUnits Кількість придбаної продукції/речовини.
 * @param nameQuantity Одиниця виміру (наприклад, "кг", "л", "шт").
 * @param amount Загальна сума витрати (вартість).
 */
data class Expense(
    val id: Int = 0,
    val hiveId: Int,
    val date: Long,
    val name: String,
    val quantityUnits: Double,
    val nameQuantity: String,
    val yearId: Int = 0,
    val amount: Double
) : Serializable