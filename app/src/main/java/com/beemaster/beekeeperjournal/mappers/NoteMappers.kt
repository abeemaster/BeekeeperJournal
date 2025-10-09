package com.beemaster.beekeeperjournal.mappers

import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.models.Note
import java.text.SimpleDateFormat
import java.util.*
import com.beemaster.beekeeperjournal.adapters.NoteSearchResult
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import com.beemaster.beekeeperjournal.models.NoteDisplayModel

/**
 * Функції-розширення для перетворення об'єктів між шарами бази даних та домену.
 */
// Функція-розширення для перетворення NoteSearchResultEntity на NoteDisplayModel
fun NoteSearchResultEntity.toNoteDisplayModel(): NoteDisplayModel {
    return NoteDisplayModel(
        id = this.id,
        text = this.content,
        type = this.type,
        hiveId = this.hiveId,
        // ✅ ВИПРАВЛЕНО: Використовуємо коректну назву поля з Entity
        hiveDisplayNumber = this.currentHiveNumber ?: "N/A",
        timestamp = this.createdAt,
        title = this.title
    )
}
/**
 * Перетворює сутність бази даних [NoteEntity] на доменну модель [Note].
 */
fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        text = this.content, // 'content' в Entity відповідає 'text' у Domain
        type = this.type,
        hiveId = this.hiveId,
        // ❌ ВИПРАВЛЕНО: Видалено 'hiveNumber', оскільки його немає у Note.kt
        timestamp = this.createdAt,
        title = this.title
    )
}

/**
 * Перетворює доменну модель [Note] на сутність бази даних [NoteEntity].
 * Використовується перед збереженням чи оновленням.
 */
fun Note.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        hiveId = this.hiveId,
        type = this.type,
        title = this.title,
        content = this.text, // 'text' у Domain відповідає 'content' в Entity
        imagePath = null, // Невідоме поле у Domain, залишаємо null/default
        createdAt = this.timestamp
    )
}

/**
 * Перетворює список сутностей [NoteEntity] на список доменних моделей [Note].
 */
fun List<NoteEntity>.toNoteList(): List<Note> {
    return this.map { it.toNote() }
}

/**
 * Допоміжна функція для форматування дати нотатки у формат "dd.MM.yyyy".
 */
fun Note.getFormattedDate(): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date(this.timestamp))
}

/**
 * Конвертує об'єкт NoteSearchResultEntity (результат DAO-запиту з JOIN)
 * у модель NoteSearchResult для відображення в адаптері результатів пошуку.
 */
fun NoteSearchResultEntity.toSearchResult(): NoteSearchResult {

    // 1. Створюємо модель Note з полів Entity
    val noteModel = Note(
        id = this.id,
        text = this.content,
        type = this.type,
        hiveId = this.hiveId,
        // ❌ ВИПРАВЛЕНО: Видалено 'hiveNumber', оскільки його немає у Note.kt
        timestamp = this.createdAt,
        title = this.title
    )

    // 2. Визначаємо відображуваний номер вулика.
    // ✅ ВИПРАВЛЕНО: Використовуємо коректну назву поля з Entity
    val displayHiveNumber: String = this.currentHiveNumber
        ?: this.hiveId.toString()

    // 3. Створюємо NoteSearchResult, передаючи Note та відображувану назву вулика.
    return NoteSearchResult(
        note = noteModel,
        hiveNumber = displayHiveNumber
    )
}