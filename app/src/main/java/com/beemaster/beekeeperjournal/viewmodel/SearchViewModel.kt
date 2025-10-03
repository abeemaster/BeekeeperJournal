package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.adapters.NoteSearchResult
import com.beemaster.beekeeperjournal.db.toSearchResult
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<NoteSearchResult>>(emptyList())
    val searchResults: StateFlow<List<NoteSearchResult>> = _searchResults.asStateFlow()

    /**
     * Виконує пошук нотаток, використовуючи оптимізований DAO-запит (SQL JOIN).
     * Результати конвертуються в NoteSearchResult за допомогою toSearchResult().
     * @param query Текст пошуку.
     */
    fun performSearch(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }

            // 1. Виклик оптимізованого репозиторію
            noteRepository.searchNotes(query).collect { results ->

                // 2. КОРЕКТНЕ ВИПРАВЛЕННЯ: Конвертуємо кожен NoteSearchResultEntity
                // у NoteSearchResult за допомогою функції-розширення.
                val searchResults = results.map { it.toSearchResult() }

                _searchResults.value = searchResults
            }
        }
    }
}