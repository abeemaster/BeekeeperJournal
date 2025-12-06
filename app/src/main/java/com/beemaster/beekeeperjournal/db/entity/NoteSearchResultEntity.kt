package com.beemaster.beekeeperjournal.db.entity

import androidx.room.ColumnInfo

/**
 * Сутність для представлення результату пошуку, що включає дані з NoteEntity
 * та актуальний номер вулика (hiveNumber) з таблиці Hives.
 * Цей клас використовується як тип повернення для DAO-запитів із JOIN.
 */
data class NoteSearchResultEntity(
    /** ID нотатки (з таблиці notes). */
    val id: Int,
    /** ID вулика (з таблиці notes) для переходу до записів вулика. */
    val hiveId: Int,
    /** Тип нотатки (наприклад, "hive", "queen"). */
    val type: String,
    /** Заголовок нотатки. */
    val title: String,
    /** Вміст нотатки. */
    val content: String,
    /** Часова мітка створення нотатки. */
    val createdAt: Long,
    /** Шлях до зображення (може бути null). */
    val imagePath: String?,
    /** Рік, до якого належить нотатка (наприклад, 2025). Використовується для фільтрації. */
    val yearId: Int,
    /** * Актуальний номер вулика, отриманий із таблиці hives за допомогою JOIN.
     * Використовується для відображення в результатах пошуку.
     */
    @ColumnInfo(name = "currentHiveDisplayNumber")
    val currentHiveNumber: String?
)