// Цей клас буде керувати даними для EditNoteActivity

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val hiveRepository: HiveRepository
) : ViewModel() {

    // ✅ ДОДАНО: Функція для завантаження оригінальної NoteEntity з правильним hiveId
    /**
     * Отримує повний об'єкт нотатки з бази даних за її ID.
     * Це необхідно для отримання коректного hiveId перед редагуванням.
     * @param noteId ID нотатки для завантаження.
     * @return Об'єкт NoteEntity або null.
     */
    suspend fun getNoteEntityById(noteId: Int): NoteEntity? {
        return noteRepository.getNoteById(noteId)
    }

    /**
     * Зберігає або оновлює нотатку в базі даних.
     * Якщо [noteId] > 0, нотатка оновлюється. Якщо [noteId] = 0, створюється нова нотатка.
     * @param noteId Унікальний ID нотатки (0 для нової).
     * @param hiveId ID вулика, до якого відноситься нотатка (0 для загальних нотаток).
     * @param type Тип запису ("hive", "queen", "notes", "general").
     * @param title Заголовок нотатки.
     * @param content Текст нотатки.
     * @param imagePath Шлях до зображення (може бути null).
     * @param createdAt Час створення нотатки (для збереження часу при оновленні).
     */
    fun saveNote(
        noteId: Int,
        hiveId: Int, // ⬅️ ПЕРЕКОНАЙТЕСЯ, ЩО ВИ ПЕРЕДАЄТЕ СЮДИ ПРАВИЛЬНИЙ ID ВУЛИКА
        type: String,
        title: String,
        content: String,
        imagePath: String?,
        createdAt: Long
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = noteId,
                hiveId = hiveId,
                type = type,
                title = title,
                content = content,
                imagePath = imagePath,
                createdAt = createdAt
            )
            // 🚀 ВИПРАВЛЕННЯ: Викликаємо UPDATE, якщо нотатка вже існує (noteId > 0)
            if (noteId > 0) {
                noteRepository.updateNote(note)
            } else {
                noteRepository.insertNote(note)
            }
        }
    }

    /**
     * Отримує об'єкт вулика за його унікальним ID.
     * Ця функція необхідна для асинхронного отримання номера вулика (hiveNumber)
     * для відображення в заголовку EditNoteActivity.
     */
    suspend fun getHiveById(hiveId: Int): HiveEntity? {
        return hiveRepository.getHiveById(hiveId)
    }
}