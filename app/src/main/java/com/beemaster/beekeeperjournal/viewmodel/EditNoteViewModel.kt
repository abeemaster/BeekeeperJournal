// Цей клас буде керувати даними для NoteActivity

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.utils.BackupPrefsManager // ✅ 1. ІМПОРТ
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val hiveRepository: HiveRepository,
    private val backupPrefsManager: BackupPrefsManager // ✅ 2. ІНЖЕКЦІЯ
) : ViewModel() {

    /**
     * Отримує повний об'єкт нотатки з бази даних за її ID.
     */
    suspend fun getNoteById(noteId: Int): Note? {
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

            val note = Note(
                id = noteId,
                type = type,
                title = title,
                text = content,
                hiveId = hiveId,
                timestamp = createdAt
            )

            if (noteId > 0) {
                noteRepository.updateNote(note)
            } else {
                noteRepository.insertNote(note)
            }

            // 3. ВИКЛИК: Оновлюємо час останньої зміни даних
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    /**
     * Отримує об'єкт вулика за його унікальним ID.
     */
    suspend fun getHiveById(hiveId: Int): HiveEntity? {
        return hiveRepository.getHiveById(hiveId)
    }

    /**
    * Видаляє нотатку з бази даних за її ID.
    */
    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }
}