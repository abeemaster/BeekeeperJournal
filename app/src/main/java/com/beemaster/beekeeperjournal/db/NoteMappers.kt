// NoteMappers.kt

// NoteMappers.kt

package com.beemaster.beekeeperjournal.db

import com.beemaster.beekeeperjournal.adapters.NoteSearchResult
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import com.beemaster.beekeeperjournal.models.Note
import java.text.SimpleDateFormat
import java.util.*

/**
 * Функція-розширення для конвертації об'єкта NoteEntity (база даних) в об'єкт Note (UI).
 */
fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        hiveNumber = this.hiveId,
        type = this.type,
        timestamp = this.createdAt,
        text = this.content,
        title = this.title
    )
}

/**
 * Функція-розширення для конвертації об'єкта Note (UI) в об'єкт NoteEntity (база даних).
 */
fun Note.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        hiveId = this.hiveNumber,
        type = this.type,
        createdAt = this.timestamp,
        content = this.text,
        imagePath = null,
        title = this.title
    )
}

// 🚀 ВИПРАВЛЕНО: Конвертує результат пошуку (Entity з JOIN) у модель для адаптера
/**
 * Конвертує об'єкт NoteSearchResultEntity (результат DAO-запиту з JOIN)
 * у модель NoteSearchResult для відображення в адаптері.
 */
fun NoteSearchResultEntity.toSearchResult(): NoteSearchResult {

    // 1. Створюємо модель Note з полів Entity
    val noteModel = Note(
        id = this.id,
        // ✅ ВИПРАВЛЕНО: Використовуємо hiveId, який є int
        text = this.content,
        type = this.type,
        hiveNumber = this.hiveId, // ПРИПУЩЕННЯ: Note.hiveNumber має тип Int
        timestamp = this.createdAt,
        title = this.title
    )

    // 2. Визначаємо відображувану назву вулика
    val displayHiveNumber: String = this.currentHiveNumber
        ?: // Запасний варіант для нотаток, що не прив'язані до вулика (hiveId == 0)
        this.hiveId.toString()

    // 3. ✅ КЛЮЧОВЕ ВИПРАВЛЕННЯ: Створюємо NoteSearchResult, передаючи Note та String.
    // Припускаємо конструктор NoteSearchResult(note: Note, hiveName: String).
    return NoteSearchResult(
        note = noteModel,
        // ПРИПУЩЕННЯ: Поле в NoteSearchResult має назву hiveNumber (String)
        hiveNumber = displayHiveNumber
    )
}


/**
 * Допоміжна функція для форматування дати.
 */
fun Note.getFormattedDate(): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date(this.timestamp))
}