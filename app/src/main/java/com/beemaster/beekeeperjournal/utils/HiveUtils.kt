// HiveUtils.kt потребує рефакторингу.

package com.beemaster.beekeeperjournal.utils

import android.content.Context
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.HiveEntity // Залишаємо імпорт, якщо HiveEntity використовується в UI/ViewModel

// Припускаємо, що R.color.color_white це необхідний default колір.

/**
 * Створює новий об'єкт HiveEntity зі стандартними значеннями.
 *
 * Примітка: Ця функція знаходиться у Utility Layer (на відміну від чистого
 * Data Layer) через пряму залежність від Android Context для отримання
 * стандартного кольору ресурсу.
 *
 * @param context Context, необхідний для отримання ресурсу кольору.
 * @param number Номер вулика, який використовується як назва та номер.
 * @return Об'єкт [HiveEntity], готовий до збереження в базу даних.
 */
fun createDefaultHiveEntity(context: Context, number: String) = HiveEntity(
    // ID буде встановлено базою даних (0 або автогенерація)
    hiveNumber = number,
    name = number,
    color = context.getColor(R.color.color_white),
    // 0 = колір за замовчуванням / прозорий / не встановлений
    secondaryColor = 0
)