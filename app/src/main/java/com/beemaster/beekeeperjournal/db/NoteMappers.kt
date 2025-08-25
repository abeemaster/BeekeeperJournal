// NoteMappers.kt

package com.beemaster.beekeeperjournal.db

import com.beemaster.beekeeperjournal.models.Note
import java.text.SimpleDateFormat
import java.util.*

/**
 * Функція-розширення для конвертації об'єкта NoteEntity (база даних) в об'єкт Note (UI).
 * Вона перетворює Long timestamp в читабельний рядок дати.
 */
fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        hiveNumber = this.hiveId,
        type = this.type,
        timestamp = this.createdAt,
        text = this.content,
        title = this.title // ✅ Додано поле title
    )
}

/**
 * Функція-розширення для конвертації об'єкта Note (UI) в об'єкт NoteEntity (база даних).
 * Вона готує дані для збереження в базу даних.
 */
fun Note.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        hiveId = this.hiveNumber,
        type = this.type,
        createdAt = this.timestamp,
        content = this.text,
        imagePath = null,
        title = this.title // ✅ Додано поле title
    )
}

/**
 * Допоміжна функція для форматування дати.
 */
fun Note.getFormattedDate(): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date(this.timestamp))
}