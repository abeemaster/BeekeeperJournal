// NoteEntity

package com.beemaster.beekeeperjournal

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val type: String,
    val date: String,
    val timestamp: Long,
    val hiveNumber: Int
) : Serializable