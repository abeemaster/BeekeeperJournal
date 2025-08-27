// У файлі HiveInfoViewModel.kt
package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.NoteEntity
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HiveInfoViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes: StateFlow<List<NoteEntity>> = _notes.asStateFlow()

    fun getNotesForHive(hiveId: Int, noteType: String) {
        viewModelScope.launch {
            noteRepository.getNotesByHiveAndType(hiveId, noteType)
                .collect { notesList ->
                    _notes.value = notesList
                }
        }
    }
}