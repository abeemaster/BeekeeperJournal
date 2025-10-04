// NoteMappers.kt

package com.beemaster.beekeeperjournal.db

import com.beemaster.beekeeperjournal.adapters.NoteSearchResult
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import com.beemaster.beekeeperjournal.models.Note
import java.text.SimpleDateFormat
import java.util.*

/**
 * Функція-розширення для конвертації об'єкта NoteEntity (база даних) в об'єкт Note (UI/модель).
 * @return Об'єкт Note для використання у шарі UI/ViewModel.
 */
fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        hiveNumber = this.hiveId, // Припускаємо, що Note.hiveNumber - це ID вулика
        type = this.type,
        timestamp = this.createdAt,
        text = this.content,
        title = this.title
    )
}

/**
 * Функція-розширення для конвертації об'єкта Note (UI/модель) в об'єкт NoteEntity (база даних).
 * @return Об'єкт NoteEntity для збереження у базі даних.
 */
fun Note.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        hiveId = this.hiveNumber,
        type = this.type,
        createdAt = this.timestamp,
        content = this.text,
        imagePath = null, // Припускаємо, що UI-модель не зберігає imagePath
        title = this.title
    )
}

/**
 * Конвертує об'єкт NoteSearchResultEntity (результат DAO-запиту з JOIN)
 * у модель NoteSearchResult для відображення в адаптері результатів пошуку.
 * @return Об'єкт NoteSearchResult з об'єднаними даними.
 */
fun NoteSearchResultEntity.toSearchResult(): NoteSearchResult {

    // 1. Створюємо модель Note з полів Entity
    val noteModel = Note(
        id = this.id,
        text = this.content,
        type = this.type,
        hiveNumber = this.hiveId,
        timestamp = this.createdAt,
        title = this.title
    )

    // 2. Визначаємо відображуваний номер вулика.
    // Якщо currentHiveNumber (з JOIN) null (наприклад, вулик видалено),
    // використовуємо hiveId як рядок.
    val displayHiveNumber: String = this.currentHiveNumber
        ?: this.hiveId.toString()

    // 3. Створюємо NoteSearchResult, передаючи Note та відображувану назву вулика.
    return NoteSearchResult(
        note = noteModel,
        hiveNumber = displayHiveNumber
    )
}


/**
 * Допоміжна функція для форматування дати нотатки у формат "dd.MM.yyyy".
 * @return Відформатований рядок дати.
 */
fun Note.getFormattedDate(): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    // Note.timestamp має бути Long (мілісекунди)
    return dateFormat.format(Date(this.timestamp))
}