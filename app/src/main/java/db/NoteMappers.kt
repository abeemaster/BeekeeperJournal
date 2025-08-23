// NoteMappers.kt

package db

import com.beemaster.beekeeperjournal.Note
import java.text.SimpleDateFormat
import java.util.*

/**
 * Функція-розширення для конвертації об'єкта NoteEntity (база даних) в об'єкт Note (UI).
 * Вона перетворює Long timestamp в читабельний рядок дати.
 */
fun NoteEntity.toNote(): Note {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(this.dateCreated))

    return Note(
        id = this.id,
        hiveNumber = this.hiveNumber,
        type = this.type,
        timestamp = this.dateCreated,
        date = dateString,
        text = this.text
    )
}

/**
 * Функція-розширення для конвертації об'єкта Note (UI) в об'єкт NoteEntity (база даних).
 * Вона готує дані для збереження в базу даних.
 */
fun Note.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        hiveNumber = this.hiveNumber,
        type = this.type,
        dateCreated = this.timestamp,
        text = this.text
    )
}