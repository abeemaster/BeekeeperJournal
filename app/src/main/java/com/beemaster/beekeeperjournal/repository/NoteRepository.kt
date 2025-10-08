// NoteRepository.kt
// Цей клас керуватиме доступом до даних нотаток, працюючи з бізнес-моделлю Note.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.mappers.toNoteEntity
import com.beemaster.beekeeperjournal.mappers.toNote
import com.beemaster.beekeeperjournal.mappers.toNoteList
import kotlinx.coroutines.flow.Flow
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import com.beemaster.beekeeperjournal.models.Note
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторій для роботи з нотатками.
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val hiveRepository: HiveRepository
) {
    /**
     * Отримує всі нотатки з бази даних у вигляді потоку Flow.
     * @return Flow, що містить список усіх Note.
     */
    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            // Мапимо Entity на Domain Model
            entities.toNoteList()
        }
    }

    /**
     * Отримує нотатки, відфільтровані за ID вулика та типом запису.
     * @param hiveId ID вулика (0 для загальних нотаток).
     * @param noteType Тип нотатки.
     * @return Flow, що містить відфільтрований список Note.
     */
    fun getNotesByHiveAndType(hiveId: Int, noteType: String): Flow<List<Note>> { // ЗМІНА ТИПУ
        return noteDao.getNotesByHiveAndType(hiveId, noteType).map { entities ->
            entities.map { it.toNote() }
        }
    }

    /**
     * Додає нову нотатку або оновлює існуючу.
     * ✅ ОБ'ЄДНАНО: Тепер ця функція відповідає за вставку (через insert, що замінює OnConflict).
     * @param note Об'єкт Note для вставки/оновлення.
     */
    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note.toNoteEntity()) // Конвертуємо Model у Entity
    }

    /**
     * Оновлює існуючу нотатку.
     * @param note Об'єкт Note для оновлення.
     */
    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toNoteEntity()) // Конвертуємо Model у Entity
    }

    /**
     * Видаляє нотатку за її унікальним ID. (Не вимагає конвертації).
     */
    suspend fun deleteNote(id: Int) {
        noteDao.deleteNote(id)
    }

    /**
     * Отримує нотатку за її унікальним ID.
     * @param id ID нотатки, яку потрібно знайти.
     * @return Об'єкт Note або null.
     */
    suspend fun getNoteById(id: Int): Note? {
        val noteEntity = noteDao.getNoteById(id)
        // ✅ ВИКОРИСТАННЯ: Конвертуємо Entity у Model
        return noteEntity?.toNote()
    }

    /**
     * ✅ ДОДАНО: Виконує ефективний пошук нотаток через DAO.
     * Цей метод є винятком, оскільки він повертає спеціальну сутність
     * `NoteSearchResultEntity` (яка містить назву вулика) для відображення у UI.
     * У цьому випадку ми не мапимо її на чисту Domain Model, оскільки вона
     * є *специфічною UI-моделлю*.
     */
    fun searchNotes(query: String): Flow<List<NoteSearchResultEntity>> {
        @Suppress("UNCHECKED_CAST")
        return noteDao.searchNotes(query) as Flow<List<NoteSearchResultEntity>>
    }


    /**
     * Імпортує список нотаток у базу даних, зазвичай, після очищення існуючих даних.
     * @param notes Список Note для імпорту.
     */
    suspend fun importNotes(notes: List<Note>) {
        // Конвертуємо Model у Entity
        val noteEntities = notes.map { it.toNoteEntity() } // Конвертуємо Model у Entity
        noteDao.clearAndInsertNotes(noteEntities)
    }


    /**
     * Отримує всі нотатки для експорту або створення бекапу.
     * @return Список об'єктів Note.
     */
    suspend fun getAllNotesForExport(): List<Note> { // ✅ ЗМІНА ТИПУ
        // Конвертуємо Entity у Model
        return noteDao.getAllNotesSuspend().map { it.toNote() }
    }
}