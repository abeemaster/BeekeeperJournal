// HiveEntity сутність, яку Room буде зберігати в базі даних. Зберігатиме дані про кожен вулик.
// Цей клас описуватиме кожен вулик.

package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hives")
data class HiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hiveNumber: String,
    val name: String,
    val color: Int,
    val secondaryColor: Int
)
