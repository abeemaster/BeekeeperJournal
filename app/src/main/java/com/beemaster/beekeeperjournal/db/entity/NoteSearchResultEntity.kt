package com.beemaster.beekeeperjournal.db.entity

import androidx.room.ColumnInfo

/**
 * Сутність для представлення результату пошуку, що включає дані з NoteEntity
 * та актуальну назву вулика (currentHiveName) з таблиці Hives.
 * Цей клас використовується як тип повернення для DAO-запитів із JOIN.
 */
data class NoteSearchResultEntity(
    // Поля з NoteEntity (ВАЖЛИВО: імена мають збігатися з NoteEntity)
    val id: Int,
    val hiveId: Int,
    val type: String,
    val title: String,
    val content: String,
    val imagePath: String?,
    val createdAt: Long,

    // Додаткове поле, яке повертає наш JOIN-запит
    // Воно має збігатися з назвою, даною в SQL-запиті: H.name AS currentHiveName
    @ColumnInfo(name = "currentHiveDisplayNumber")
    val currentHiveNumber: String? // Може бути null, якщо це загальний запис (hiveId=0)
)