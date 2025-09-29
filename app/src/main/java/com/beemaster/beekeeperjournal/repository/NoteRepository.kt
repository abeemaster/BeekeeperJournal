// NoteRepository.kt Ці класи будуть керувати доступом до даних.
// Сюди винесено всю логіку роботи з файлами нотаток.

package com.beemaster.beekeeperjournal.repository

import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
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
     * Використовується для експорту або загального відображення даних.
     * @return Flow, що містить список усіх NoteEntity.
     */
    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    /**
     * Отримує нотатки, відфільтровані за ID вулика та типом запису.
     * Використовується для відображення даних у HiveInfoActivity та EditNoteActivity.
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
     * Отримує всі нотатки. Використовується у синхронному контексті (наприклад, для експорту).
     * @return Список усіх NoteEntity.
     */
    suspend fun getAllNotesSuspend(): List<NoteEntity> {
        return noteDao.getAllNotesSuspend()
    }

    /**
     * Імпортує список нотаток у базу даних, зазвичай, після очищення існуючих даних.
     * @param notes Список NoteEntity для імпорту.
     */
    suspend fun importNotes(notes: List<NoteEntity>) {
        // Припускаємо, що noteDao.clearAndInsertNotes містить логіку очищення та вставки.
        noteDao.clearAndInsertNotes(notes)
    }
}