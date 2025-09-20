package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.db.toNote
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.adapters.NoteSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val hiveRepository: HiveRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<NoteSearchResult>>(emptyList())
    val searchResults: StateFlow<List<NoteSearchResult>> = _searchResults.asStateFlow()

    fun performSearch(query: String) {
        viewModelScope.launch {
            noteRepository.getAllNotes().collect { notes ->
                val allHives = hiveRepository.getAllHives()
                val filteredNotes = if (query.isBlank()) {
                    emptyList()
                } else {
                    notes.filter { noteEntity ->
                        noteEntity.content.contains(query, ignoreCase = true) ||
                                noteEntity.createdAt.toString().contains(query, ignoreCase = true) ||
                                noteEntity.type.contains(query, ignoreCase = true) ||
                                (noteEntity.hiveId.toString() == query && noteEntity.type != "general") ||
                                (noteEntity.type == "hive" && allHives.find { it.id == noteEntity.hiveId }?.name?.contains(query, ignoreCase = true) == true)
                    }
                }

                val results = filteredNotes.map { noteEntity ->
                    val hiveName = if (noteEntity.type == "general") {
                        "Загальні записи"
                    } else {
                        val foundHive = allHives.find { it.id == noteEntity.hiveId }
                        foundHive?.name ?: "Вулик №${noteEntity.hiveId}"
                    }
                    NoteSearchResult(noteEntity.toNote(), hiveName)
                }
                _searchResults.value = results
            }
        }
    }
}