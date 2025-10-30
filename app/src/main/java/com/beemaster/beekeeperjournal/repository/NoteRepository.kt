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
import com.beemaster.beekeeperjournal.models.NoteDisplayModel // ✅ НОВИЙ ІМПОРТ
import com.beemaster.beekeeperjournal.mappers.toNoteDisplayModel // ✅ НОВИЙ ІМПОРТ
import kotlinx.coroutines.flow.first // ДОДАНО: Для перетворення Flow на List
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
    fun getNotesByHiveAndType(hiveId: Int, noteType: String): Flow<List<Note>> { // ЦЯ ФУНКЦІЯ ПОВЕРТАЄ ЧИСТУ ДОМЕННУ МОДЕЛЬ
        return noteDao.getNotesByHiveAndType(hiveId, noteType).map { entities ->
            entities.map { it.toNote() }
        }
    }

    /**
     * ✅ НОВА ФУНКЦІЯ: Отримує нотатки, збагачені номером вулика, для відображення в UI.
     * Ця функція замінює getNotesByHiveAndType для використання у HiveInfoViewModel.
     * @param hiveId ID вулика (0 для загальних нотаток).
     * @param noteType Тип нотатки.
     * @return Flow, що містить відфільтрований список NoteDisplayModel.
     */
    fun getNotesForHiveDisplay(hiveId: Int, noteType: String): Flow<List<NoteDisplayModel>> {
        // Використовуємо searchNotes (який робить JOIN і повертає NoteSearchResultEntity)
        // Для live-оновлень тут необхідний Flow, тому ми використовуємо Flow
        return noteDao.searchNotes(query = "")
            .map { searchResults ->
                searchResults
                    // Фільтруємо на рівні репозиторію за hiveId та type.
                    .filter { it.hiveId == hiveId && it.type == noteType }
                    // ✅ КОНВЕРТУЄМО: Використовуємо мапер для перетворення на NoteDisplayModel
                    .map { it.toNoteDisplayModel() }
            }
    }

    /**
     * Виконує ефективний пошук нотаток через DAO.
     * Тепер це suspend-функція, яка збирає (collects) перше значення з Flow.
     */
    suspend fun searchNotes(query: String): List<NoteSearchResultEntity> {
        // Ми беремо лише ПЕРШИЙ (і єдиний) випуск Flow, що перетворює Flow<List<...>> на List<...>.
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
     * Оновлює вміст (текст) нотатки за її ID.
     * @param noteId ID нотатки, яку потрібно оновити.
     * @param newContent Новий текст нотатки.
     */
    suspend fun updateNoteContent(noteId: Int, newContent: String) {
        noteDao.updateNoteContent(noteId, newContent)
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
        // Ми припускаємо, що NoteDao має метод для отримання NoteSearchResultEntity за ID.
        // Якщо такого методу в DAO немає, вам потрібно буде його створити,
        // або отримати NoteEntity та самостійно додати hiveNumber з HiveRepository.

        // Для спрощення, припустимо, що DAO повертає NoteSearchResultEntity
        val searchResult = noteDao.getNoteSearchResultById(id)
        return searchResult?.toNoteDisplayModel()
    }

      /**
     * Отримує всі нотатки для експорту або створення бекапу.
     * @return Список об'єктів Note.
     */
    suspend fun getAllNotesForExport(): List<Note> {
        return noteDao.getAllNotesSuspend().map { it.toNote() }
    }
}