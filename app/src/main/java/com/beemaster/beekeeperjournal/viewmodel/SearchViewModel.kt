package com.beemaster.beekeeperjournal.viewmodel

import android.util.Log
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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<NoteSearchResult>>(emptyList())
    val searchResults: StateFlow<List<NoteSearchResult>> = _searchResults.asStateFlow()

    // ✅ 1. ДОДАНО: Стан, що відстежує факт ініціації пошуку користувачем.
    private val _isSearchPerformed = MutableStateFlow(false)

    /**
     * Виконує пошук нотаток.
     * @param query Текст пошуку.
     */

    // ...
    fun performSearch(query: String) {
        viewModelScope.launch {
            Log.d("SEARCH_FLOW", "ViewModel: performSearch розпочато для запиту: '$query'")

            // ✅ 1. Встановлюємо TRUE на початку
            _isSearchPerformed.value = true
            Log.d("SEARCH_FLOW", "ViewModel: _isSearchPerformed встановлено на TRUE")

            if (query.isBlank()) {
                // ❌ ВИДАЛЕНО: return@launch (бо він не давав оновити Flow, якщо значення не змінилося)

                // 2. Якщо запит пустий, отримуємо порожній список.
                val emptyListResults = emptyList<NoteSearchResultEntity>()
                val searchResults = emptyListResults.map { it.toSearchResult() }

                _searchResults.value = searchResults
                Log.d("SEARCH_FLOW", "ViewModel: Запит пустий. StateFlow оновлено. Кінець.")
                return@launch // Тепер можна вийти, коли все оновлено
            }

            // ... (Далі йде логіка для непустого запиту)
            val results = noteRepository.searchNotes(query)

            Log.d("SEARCH_FLOW", "ViewModel: Отримано результатів від репозиторію: ${results.size}")

            val searchResults = results.map { it.toSearchResult() }

            _searchResults.value = searchResults
            Log.d("SEARCH_FLOW", "ViewModel: StateFlow оновлено. Кінець.")
        }
    }

    /**
     * Функція, яку Activity викликає для перевірки стану.
     */
    /**
     * ✅ 3. ДОДАНО: Функція для перевірки стану
     */
    fun isSearchPerformed(): Boolean {
        // Лог для перевірки, що Activity дійсно викликає цей метод
        Log.d("SEARCH_FLOW", "ViewModel: isSearchPerformed() викликано. Повертає: ${_isSearchPerformed.value}")
        return _isSearchPerformed.value
    }
}
