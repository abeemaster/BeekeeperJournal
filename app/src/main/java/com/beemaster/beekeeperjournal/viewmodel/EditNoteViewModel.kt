// Цей клас буде керувати даними для EditNoteActivity

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.models.Note
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

    // ✅ ВИПРАВЛЕНО: Назва методу уніфікована, повертає Note?
    /**
     * Отримує повний об'єкт нотатки з бази даних за її ID.
     */
    suspend fun getNoteById(noteId: Int): Note? {
        // noteRepository.getNoteById тепер повертає Note
        return noteRepository.getNoteById(noteId)
    }

    /**
     * Зберігає або оновлює нотатку в базі даних.
     */
    fun saveNote(
        noteId: Int,
        hiveId: Int,
        type: String,
        title: String,
        content: String,
        createdAt: Long
    ) {
        viewModelScope.launch {

            // 1. ✅ ВИПРАВЛЕНО: Отримуємо hiveNumber, необхідний для конструктора Note
            // Це вимагає suspend-виклик, який можливий всередині viewModelScope.launch.
            val hive = hiveRepository.getHiveById(hiveId)
            val hiveNumber = hive?.hiveNumber ?: ""

            // 2. Створюємо Domain Model (Note), використовуючи НОВІ назви полів
            val note = Note(
                id = noteId,
                type = type,
                title = title,
                text = content,
                hiveId = hiveId,
                timestamp = createdAt,
                hiveNumber = hiveNumber
            )

            // Репозиторій тепер приймає Note
            if (noteId > 0) {
                noteRepository.updateNote(note)
            } else {
                noteRepository.insertNote(note)
            }
        }
    }

    /**
     * Отримує об'єкт вулика за його унікальним ID.
     */
    suspend fun getHiveById(hiveId: Int): HiveEntity? {
        return hiveRepository.getHiveById(hiveId)
    }

    /**
     * Видаляє нотатку за її ID.
     */
    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }
}