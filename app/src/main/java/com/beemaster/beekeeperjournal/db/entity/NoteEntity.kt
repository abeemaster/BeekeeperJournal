package com.beemaster.beekeeperjournal.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Клас-сутність для збереження нотаток (записів інспекцій, загальних нотаток тощо).
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    /** Унікальний ідентифікатор нотатки. Генерується автоматично. */
    val id: Int = 0,
    /** ID вулика, до якого прив'язана нотатка. */
    val hiveId: Int,
    /** Тип нотатки (наприклад, "hive", "queen", "general"). */
    val type: String,
    /** Заголовок нотатки. */
    val title: String,
    /** Основний зміст нотатки. */
    val content: String,
    /** Часова мітка створення нотатки у форматі Unix timestamp (Long). */
    val createdAt: Long,
    /** Шлях до зображення, пов'язаного з нотаткою (може бути null). */
    val imagePath: String?
)