// NoteRepository.kt Ці класи будуть керувати доступом до даних.
// Сюди винесено всю логіку роботи з файлами нотаток.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity // ✅ ДОДАНО: Імпорт нової сутності
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторій для роботи з нотатками.
 * Відповідає за абстрагування джерела даних (NoteDao) від рівня ViewModel.
 * Використовує анотацію @Singleton, щоб Hilt надавав єдиний екземпляр класу.
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    /**
     * Отримує всі нотатки з бази даних у вигляді потоку Flow.
     * @return Flow, що містить список усіх NoteEntity.
     */
    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    /**
     * Отримує нотатки, відфільтровані за ID вулика та типом запису.
     * @param hiveId ID вулика (0 для загальних нотаток).
     * @param noteType Тип нотатки ("hive", "queen", "notes" або "general").
     * @return Flow, що містить відфільтрований список NoteEntity.
     */
    fun getNotesByHiveAndType(hiveId: Int, noteType: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesByHiveAndType(hiveId, noteType)
    }

    /**
     * Додає нову нотатку або оновлює існуючу.
     * @param note Об'єкт NoteEntity для вставки/оновлення.
     */
    suspend fun insertNote(note: NoteEntity) {
        noteDao.insertNote(note)
    }

    /**
     * Оновлює існуючу нотатку.
     * @param note Об'єкт NoteEntity для оновлення.
     */
    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    /**
     * Видаляє нотатку за її унікальним ID.
     * @param id ID нотатки для видалення.
     */
    suspend fun deleteNote(id: Int) {
        noteDao.deleteNote(id)
    }

    /**
     * Отримує нотатку за її унікальним ID.
     * @param id ID нотатки, яку потрібно знайти.
     * @return Об'єкт NoteEntity або null.
     */
    suspend fun getNoteById(id: Int): NoteEntity? {
        return noteDao.getNoteById(id)
    }

    /**
     * ✅ ДОДАНО: Виконує ефективний пошук нотаток через DAO.
     * @param query Текст для пошуку.
     * @return Flow, що містить список NoteSearchResultEntity з актуальною назвою вулика.
     */
    fun searchNotes(query: String): Flow<List<NoteSearchResultEntity>> {
        // ПРИМІТКА: Вам потрібно буде замінити List<NoteEntity> у NoteDao.kt на
        // Flow<List<NoteSearchResultEntity>> після того, як ви створили клас NoteSearchResultEntity.
        @Suppress("UNCHECKED_CAST")
        return noteDao.searchNotes(query) as Flow<List<NoteSearchResultEntity>>
    }


    /**
     * ❌ ВИДАЛЕНО: Цей метод є надлишковим.
     * Якщо потрібні дані без Flow, використовуйте getAllNotes().first() у ViewModel.
     * suspend fun getAllNotesSuspend(): List<NoteEntity> {
     * return noteDao.getAllNotesSuspend()
     * }
     */

    /**
     * Імпортує список нотаток у базу даних, зазвичай, після очищення існуючих даних.
     * @param notes Список NoteEntity для імпорту.
     */
    suspend fun importNotes(notes: List<NoteEntity>) {
        noteDao.clearAndInsertNotes(notes)
    }
}