// NoteMappers.kt

package com.beemaster.beekeeperjournal.mappers

import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.models.Note
import java.text.SimpleDateFormat
import java.util.*
import com.beemaster.beekeeperjournal.adapters.NoteSearchResult
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity

/**
 * Функції-розширення для перетворення об'єктів між шарами бази даних та домену.
 */

/**
 * Перетворює сутність бази даних [NoteEntity] на доменну модель [Note].
 *
 * ПРИМІТКА: Цей мапер зараз використовує заглушку для 'hiveNumber' ("?")
 * оскільки репозиторій повертає лише NoteEntity, яка не містить рядкового номера вулика.
 * Усі нотатки, які йдуть до UI, мають бути 'збагачені' номером вулика
 * пізніше (наприклад, у репозиторії або ViewModel).
 */
fun NoteEntity.toNote(): Note {
    return Note(
        id = this.id,
        text = this.content, // 'content' в Entity відповідає 'text' у Domain
        type = this.type,
        hiveId = this.hiveId, // ✅ ПРАВИЛЬНЕ ПЕРЕТВОРЕННЯ
        hiveNumber = "?", // ЗАГЛУШКА: Рядковий номер вулика не зберігається в Entity
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
        hiveId = this.hiveId, // ✅ ПРАВИЛЬНЕ ПЕРЕТВОРЕННЯ
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
 * Це функція-розширення для доменної моделі [Note].
 * @return Відформатований рядок дати.
 */
fun Note.getFormattedDate(): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    // Note.timestamp має бути Long (мілісекунди)
    return dateFormat.format(Date(this.timestamp))
}

/**
 * Конвертує об'єкт NoteSearchResultEntity (результат DAO-запиту з JOIN)
 * у модель NoteSearchResult для відображення в адаптері результатів пошуку.
 * @return Об'єкт NoteSearchResult з об'єднаними даними.
 */
fun NoteSearchResultEntity.toSearchResult(): NoteSearchResult {

    // 1. Створюємо модель Note з полів Entity
    // ПРИМІТКА: Ми використовуємо NoteSearchResultEntity напряму, оскільки він містить
    // спеціальні поля для пошуку (наприклад, номер вулика).
    val noteModel = Note(
        id = this.id,
        text = this.content,
        type = this.type,
        hiveId = this.hiveId, // ✅ ВИКОРИСТОВУЄМО ПРАВИЛЬНИЙ ID!
        hiveNumber = this.currentHiveNumber ?: this.hiveId.toString(), // Використовуємо реальний номер або ID як заглушку
        timestamp = this.createdAt,
        title = this.title
    )

    // 2. Визначаємо відображуваний номер вулика.
    val displayHiveNumber: String = this.currentHiveNumber
        ?: this.hiveId.toString()

    // 3. Створюємо NoteSearchResult, передаючи Note та відображувану назву вулика.
    return NoteSearchResult(
        note = noteModel,
        hiveNumber = displayHiveNumber
    )
}