// HiveEntity сутність, яку Room буде зберігати в базі даних. Зберігатиме дані про кожен вулик.

package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Клас-сутність для збереження інформації про вулик.
 * Кожен об'єкт відповідає одному вулику у пасіці.
 */
@Entity(tableName = "hives")
data class HiveEntity(
    @PrimaryKey(autoGenerate = true)
    /** Унікальний ідентифікатор вулика. Генерується автоматично. */
    val id: Int = 0,
    /** Номер вулика, який використовується для його ідентифікації (наприклад, "А1", "35"). */
    val hiveNumber: String,
    /** Основний колір вулика, збережений як Int-ресурс або значення кольору. */
    val color: Int,
    /** Додатковий (вторинний) колір вулика, збережений як Int-ресурс або значення кольору. */
    val secondaryColor: Int,

    // Нові поля для паспорта матки:
    /** Рік початку яйцекладки матки */
    val queenYear: String? = null,
    /** Порода матки */
    val queenBreed: String? = null,
    /** Короткі примітки (зовнішній вигляд тощо) */
    val queenNotes: String? = null
)
