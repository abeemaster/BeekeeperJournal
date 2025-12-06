// Income.kt - Чиста доменна модель

package com.beemaster.beekeeperjournal.models

import java.io.Serializable

/**
 * Клас даних, що представляє чисту доменну модель Прибутку.
 * Ця модель використовується у шарах Domain та Presentation (ViewModel).
 *
 * @param id Унікальний ідентифікатор прибутку (0 для нового).
 * @param hiveId ID вулика, до якого належить цей прибуток.
 * @param date Дата прибутку у форматі Unix timestamp (мілісекунди).
 * @param productName Назва проданої продукції.
 * @param quantity Кількість проданої продукції.
 * @param unitName Одиниця виміру.
 * @param price Ціна за одну одиницю продукції.
 * @param totalAmount Загальна сума, отримана з продажу.
 */
data class Income(
    val id: Int = 0,
    val hiveId: Int,
    val date: Long,
    val productName: String,
    val quantity: Double,
    val unitName: String,
    val price: Double,
    val yearId: Int = 0,
    val totalAmount: Double
) : Serializable