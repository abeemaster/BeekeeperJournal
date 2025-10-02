// У файлі HiveInfoViewModel.kt
package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.repository.HiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HiveInfoViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val hiveRepository: HiveRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes: StateFlow<List<NoteEntity>> = _notes.asStateFlow()

    suspend fun getHiveById(hiveId: Int) = hiveRepository.getHiveById(hiveId)
    fun getNotesForHive(hiveId: Int, noteType: String) {
        viewModelScope.launch {
            noteRepository.getNotesByHiveAndType(hiveId, noteType)
                .collect { notesList ->
                    _notes.value = notesList
                }
        }
    }

    // ✅ Додаємо нову функцію для видалення нотатки
    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note.id)
        }
    }


}