// HiveCreator.kt (В Data Layer або Domain Layer)

package com.beemaster.beekeeperjournal.data

import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Клас, відповідальний за створення нового об'єкта HiveEntity зі стандартними значеннями.
 * Ця логіка тепер не залежить від Android Context.
 */
@Singleton
class HiveCreator @Inject constructor(
    // ✅ Ін'єкція фіксованого значення кольору
    @Named("DefaultHiveColor") private val defaultColor: Int
) {
    /**
     * Створює новий об'єкт HiveEntity зі стандартними значеннями.
     * @param number Номер вулика, який використовується як назва та номер.
     * @return Об'єкт [HiveEntity], готовий до збереження в базу даних.
     */
    fun createDefaultHiveEntity(number: String) = HiveEntity(
        // ID буде встановлено базою даних (0 або автогенерація)
        hiveNumber = number,
        name = number,
        // ✅ Використовуємо ін'єктовану константу
        color = defaultColor,
        // 0 = колір за замовчуванням / прозорий / не встановлений
        secondaryColor = 0
    )
}