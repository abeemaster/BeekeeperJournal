// NoteRepository.kt
// Цей клас керуватиме доступом до даних нотаток, працюючи з бізнес-моделлю Note.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.mappers.toNoteEntity
import com.beemaster.beekeeperjournal.mappers.toNote
import com.beemaster.beekeeperjournal.mappers.toNoteList
import kotlinx.coroutines.flow.Flow
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import com.beemaster.beekeeperjournal.mappers.toNoteDisplayModel
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.models.NoteDisplayModel // ✅ НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.first // Для перетворення Flow на List
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Репозиторій для роботи з нотатками.
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    @Suppress("unused") private val hiveRepository: HiveRepository
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
     * Отримує нотатки, збагачені номером вулика, для відображення в UI.
     * Ця функція замінює getNotesByHiveAndType для використання у HiveInfoViewModel.
     * @param hiveId ID вулика (0 для загальних нотаток).
     * @param noteType Тип нотатки.
     * @return Flow, що містить відфільтрований список NoteDisplayModel.
     */
    fun getNotesForHiveDisplay(hiveId: Int, noteType: String): Flow<List<NoteDisplayModel>> {
        return noteDao.searchNotes(query = "")
            .map { searchResults ->
                searchResults
                    .filter { it.hiveId == hiveId && it.type == noteType }
                    .map { it.toNoteDisplayModel() }
            }
    }

    /**
     * Виконує ефективний пошук нотаток через DAO.
     * Тепер це suspend-функція, яка збирає (collects) перше значення з Flow.
     */
    /**
     * Виконує ефективний пошук нотаток та повертає List<NoteSearchResultEntity>.
     * Перетворено на suspend-функцію для використання у ViewModel.
     */

    suspend fun searchNotes(query: String): List<NoteSearchResultEntity> {
        @Suppress("UNCHECKED_CAST")
        return noteDao.searchNotes(query).first()
    }

    /**
     * Імпортує список нотаток у базу даних, зазвичай, після очищення існуючих даних.
     * @param notes Список Note для імпорту.
     */
    suspend fun importNotes(notes: List<Note>) {
        val noteEntities = notes.map { it.toNoteEntity() }
        noteDao.clearAndInsertNotes(noteEntities)
    }

    /**
     * Додає нову нотатку або оновлює існуючу.
     * @param note Об'єкт Note для вставки/оновлення.
     */
    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note.toNoteEntity())
    }

    /**
     * Оновлює існуючу нотатку.
     * @param note Об'єкт Note для оновлення.
     */
    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toNoteEntity())
    }

    /**
     * Видаляє нотатку за її унікальним ID.
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
        return noteEntity?.toNote()
    }

    /**
     * Отримує повну модель відображення нотатки, збагачену номером вулика, за її ID.
     * @param id ID нотатки.
     * @return Об'єкт [NoteDisplayModel] або null.
     */
    suspend fun getNoteDisplayModelById(id: Int): NoteDisplayModel? {
        val searchResult = noteDao.getNoteSearchResultById(id)
        return searchResult?.toNoteDisplayModel()
    }

}