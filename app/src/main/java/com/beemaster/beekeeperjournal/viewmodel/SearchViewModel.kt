package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.adapters.NoteSearchResult
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import com.beemaster.beekeeperjournal.mappers.toSearchResult
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first


@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _screenState = MutableStateFlow<SearchScreenState>(SearchScreenState.Initial)
    val screenState: StateFlow<SearchScreenState> = _screenState.asStateFlow()

    fun performSearch(query: String) {
        viewModelScope.launch {
            _screenState.value = SearchScreenState.Loading

            // ... (перевірка query.isBlank())

            // Отримуємо одноразовий знімок результатів з Flow
            // results тут має тип List<NoteSearchResultEntity>
            val results = noteRepository.searchNotes(query).first()

            // ЗМІНА ТУТ: Явно вказуємо тип NoteSearchResultEntity для параметра 'it'
            val searchResults = results.map { it: NoteSearchResultEntity ->
                it.toSearchResult()
            }

            _screenState.value = SearchScreenState.Results(
                list = searchResults,
                queryWasExecuted = true
            )
        }
    }

    fun getCurrentResults(): List<NoteSearchResult> {
        return when (val state = _screenState.value) {
            is SearchScreenState.Results -> state.list
            else -> emptyList()
        }
    }
}