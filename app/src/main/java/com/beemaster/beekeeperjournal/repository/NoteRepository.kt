// NoteRepository.kt
// Цей клас керуватиме доступом до даних нотаток, працюючи з бізнес-моделлю Note.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.mappers.toNoteEntity
import com.beemaster.beekeeperjournal.mappers.toNote
import com.beemaster.beekeeperjournal.mappers.toNoteList
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import com.beemaster.beekeeperjournal.mappers.toNoteDisplayModel
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.models.NoteDisplayModel
import com.beemaster.beekeeperjournal.utils.YearPrefsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Репозиторій для роботи з нотатками.
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val yearPrefsManager: YearPrefsManager, // 1. ІНЖЕКЦІЯ МЕНЕДЖЕРА
    @Suppress("unused") private val hiveRepository: HiveRepository
) {

    /**
     * Отримує поточний обраний користувачем ID року.
     */
    fun getCurrentYearId(): Int {
        return yearPrefsManager.activeYearId.value.toInt()
    }

    /**
     * Отримує всі нотатки з бази даних у вигляді потоку Flow, фільтруючи за активним роком.
     *
     * Активний yearId береться з yearPrefsManager.
     * @return Flow, що містить список усіх Note.
     */
    fun getAllNotes(): Flow<List<NoteDisplayModel>> { // ЗМІНЕНО: повертає NoteDisplayModel
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                // Передаємо ID активного року в DAO
                noteDao.getAllNotes(yearIdLong.toInt())
            }
            .map { searchResults -> // searchResults: List<NoteSearchResultEntity>
                // Використовуємо коректний мапер: NoteSearchResultEntity -> NoteDisplayModel
                searchResults.map { it.toNoteDisplayModel() }
            }
    }
//---------------
    /**
     * Отримує нотатки для конкретного вулика та типу запису (hive, queen, general).
     * @param hiveId ID вулика (0 для загальних нотаток).
     * @param noteType Тип запису (наприклад, "hive", "queen").
     * @return Flow зі списком об'єктів NoteDisplayModel.
     */
    fun getNotesForHiveDisplay(hiveId: Int, noteType: String): Flow<List<NoteDisplayModel>> {
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                // Викликаємо метод DAO, який включає фільтр по noteType
                noteDao.getNotesForHiveAndType(hiveId, noteType, yearIdLong.toInt())
            }
            .map { searchResults -> // searchResults: List<NoteSearchResultEntity>
                // Конвертуємо результати пошуку в UI-модель
                searchResults.map { it.toNoteDisplayModel() }
            }
    }

    /**
     * Отримує повну модель відображення нотатки за її ID.
     * Використовується для редагування.
     * @param noteId ID нотатки.
     * @return Об'єкт [NoteDisplayModel] або null.
     */
    suspend fun getNoteDisplayModelById(noteId: Int): NoteDisplayModel? {
        val resultEntity = noteDao.getNoteSearchResultById(noteId) // Цей метод повинен бути в DAO
        return resultEntity?.toNoteDisplayModel()
    }
    /**
     * Отримує всі нотатки, пов'язані з конкретним вуликом, фільтруючи за активним роком.
     */
    fun getNotesByHiveId(hiveId: Int): Flow<List<Note>> {
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                // Передаємо ID вулика та ID активного року в DAO
                noteDao.getNotesByHiveId(hiveId, yearIdLong.toInt())
            }
            .map { noteEntities -> // noteEntities: List<NoteEntity>
                // Використовуємо коректний мапер: NoteEntity -> Note
                noteEntities.map { it.toNote() }
            }
    }

    /**
     * Шукає нотатки за текстом та номером вулика, фільтруючи за активним роком.
     */
    fun searchNotes(query: String): Flow<List<NoteSearchResultEntity>> {
        return yearPrefsManager.activeYearId
            .flatMapLatest { yearIdLong ->
                // noteDao.searchNotes() повертає Flow<List<NoteSearchResultEntity>>
                noteDao.searchNotes(query, yearIdLong.toInt())
            }
        // ВИДАЛЯЄМО зайвий .map {} блок, який конвертував у NoteDisplayModel
    }

    // ----------------------------------------------------------------------------------
    // CRUD Operations
    // ----------------------------------------------------------------------------------

    /**
     * Додає нову нотатку або оновлює існуючу.
     * Присвоює активний yearId, якщо він ще не встановлений (yearId == 0).
     * @param note Об'єкт Note для вставки/оновлення.
     */
    suspend fun insertNote(note: Note) {
        val noteWithYearId = if (note.yearId == 0) {
            val currentYearId = yearPrefsManager.activeYearId.first().toInt()
            note.copy(yearId = currentYearId)
        } else {
            note
        }
        noteDao.insertNote(noteWithYearId.toNoteEntity())
    }

    /**
     * Оновлює існуючу нотатку.
     * @param note Об'єкт Note для оновлення.
     */
    suspend fun updateNote(note: Note) {
        // При оновленні yearId, як правило, вже встановлений.
        noteDao.updateNote(note.toNoteEntity())
    }

    /**
     * Видаляє нотатку за її унікальним ID.
     */
    suspend fun deleteNote(id: Int) {
        noteDao.deleteNote(id)
    }

    /**
     * Видаляє всі нотатки, пов'язані з певним пасічним роком.
     * @param yearId ID року для видалення.
     * @return Кількість видалених нотаток.
     */
    suspend fun deleteNotesByYearId(yearId: Long): Int { // 👈 НОВА ФУНКЦІЯ
        return noteDao.deleteNotesByYearId(yearId)
    }

    /**
     * Отримує всі нотатки як статичний список (наприклад, для експорту).
     * Фільтрація за роком тут не потрібна, оскільки експорт, ймовірно, має включати всі роки.
     * @return Список усіх Note.
     */
    suspend fun getAllNotesSuspend(): List<Note> {
        val noteEntities = noteDao.getAllNotesSuspend()
        return noteEntities.toNoteList()
    }

    /**
     * Отримує всі нотатки без фільтрації за роком. Використовується виключно для експорту (бекапу).
     * @return Список усіх Note.
     */
    suspend fun getAllNotesForExportSuspend(): List<Note> {
        return noteDao.getAllNotesForExport().map { it.toNote() }
    }

    /**
     * Виконує очищення таблиці нотаток та подальшу вставку нового списку
     * в рамках однієї атомарної транзакції (для імпорту/відновлення), після очищення існуючих даних.
     * @param notes Список Note для імпорту.
     */
    suspend fun importNotes(notes: List<Note>) {
        val noteEntities = notes.map { it.toNoteEntity() }
        noteDao.clearAndInsertNotes(noteEntities)
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
}