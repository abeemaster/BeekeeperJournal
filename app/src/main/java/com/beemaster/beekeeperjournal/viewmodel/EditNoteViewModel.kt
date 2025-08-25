// EditNoteViewModel

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.NoteEntity
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    fun saveNote(
        noteId: Int?,
        hiveId: Int,
        type: String,
        title: String,
        content: String,
        imagePath: String?
    ) {
        viewModelScope.launch {
            if (noteId == null || noteId == 0) {
                // Створюємо нову нотатку
                val newNote = NoteEntity(
                    hiveId = hiveId,
                    type = type,
                    title = title,
                    content = content,
                    createdAt = System.currentTimeMillis(), // ✅ Виправлено: використовуємо Long
                    imagePath = imagePath
                )
                noteRepository.insertNote(newNote)
            } else {
                // Оновлюємо існуючу нотатку
                val existingNote = noteRepository.getNoteById(noteId)
                existingNote?.let {
                    val updatedNote = it.copy(
                        title = title,
                        content = content,
                        imagePath = imagePath,
                        createdAt = it.createdAt
                    )
                    noteRepository.updateNote(updatedNote)
                }
            }
        }
    }
}