// HiveEntity

package com.beemaster.beekeeperjournal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hives")
data class HiveEntity(
    @PrimaryKey(autoGenerate = false) val number: Int,
    val name: String
)