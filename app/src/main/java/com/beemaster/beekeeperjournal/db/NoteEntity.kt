// NoteEntity сутність, яку Room буде зберігати в базі даних. Зберігатиме дані про вулики.
// Цей клас описуватиме кожну нотатку.

package com.beemaster.beekeeperjournal.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hiveId: Int,
    val type: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val imagePath: String?
)