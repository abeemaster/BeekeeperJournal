package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.beemaster.beekeeperjournal.db.Converters

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