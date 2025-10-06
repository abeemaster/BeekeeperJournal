// NoteManager.kt  відповідає за всю логіку роботи з даними (читання, запис, оновлення, видалення).

package com.beemaster.beekeeperjournal.utils

import android.util.Log
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Клас, що керує логікою збереження, оновлення, видалення та завантаження записів.
 * Він надається Hilt як Singleton, що гарантує єдиний екземпляр.
 */
// У файлі NoteManager.kt

@Singleton
class NoteManager @Inject constructor(
    private val noteRepository: NoteRepository,
    private val hiveRepository: HiveRepository
) {
    companion object {
        const val TAG = "NoteManager"
    }

    /**
     * Асинхронно отримує всі записи для певного вулика та типу.
     * Повертає Flow для реактивного UI.
     *
     * @param entryType Тип записів (наприклад, "notes", "queen", "general").
     * @param hiveId Ідентифікатор вулика.
     * @return Flow зі списком об'єктів NoteEntity.
     */
    // ✅ Змінено: hiveId тепер Int
    fun loadNotes(entryType: String, hiveId: Int): Flow<List<NoteEntity>> {
        return noteRepository.getNotesByHiveAndType(hiveId, entryType)
    }

    /**
     * Асинхронно отримує запис за його ідентифікатором.
     *
     * @param noteId Ідентифікатор запису.
     * @return Об'єкт NoteEntity або null, якщо не знайдено.
     */
    suspend fun getNoteById(noteId: Int): Note? {
        return noteRepository.getNoteById(noteId)
    }

    /**
     * Асинхронно видаляє запис за його ідентифікатором.
     *
     * @param noteId Ідентифікатор запису для видалення.
     */
    suspend fun deleteNote(noteId: Int) {
        try {
            noteRepository.deleteNote(noteId)
            Log.d(TAG, "Запис з ID $noteId успішно видалено.")
        } catch (e: Exception) {
            Log.e(TAG, "Помилка при видаленні запису: ${e.message}", e)
            throw e // ✅ Викидаємо виняток, щоб ViewModel міг його обробити
        }
    }
}