// EditNoteViewModel

// У файлі EditNoteViewModel.kt
package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    // ✅ Оновлено: додано параметр createdAt
    fun saveNote(
        noteId: Int,
        hiveId: Int,
        type: String,
        title: String,
        content: String,
        imagePath: String?,
        createdAt: Long // ✅ Додано параметр createdAt
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = noteId,
                hiveId = hiveId,
                type = type,
                title = title,
                content = content,
                imagePath = imagePath,
                createdAt = createdAt // ✅ Використовуємо передане значення
            )
            noteRepository.insertNote(note)
        }
    }
}