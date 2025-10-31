// SearchScreenState.kt файл для опису всіх можливих станів екрана Пошуку.
// Створений для заповнення спеціального початкового значення Flow.

package com.beemaster.beekeeperjournal.viewmodel

import com.beemaster.beekeeperjournal.adapters.NoteSearchResult

/**
 * Описує можливі стани екрана пошуку.
 */
sealed class SearchScreenState {
    /** Стан, що відображається перед першим виконанням пошуку (наприклад, пустий екран/запрошення). */
    object Initial : SearchScreenState()
    /** Стан завантаження (якщо пошук триває). */
    object Loading : SearchScreenState()
    /** Результат успішного або невдалого пошуку. */
    data class Results(
        val list: List<NoteSearchResult>,
        val queryWasExecuted: Boolean // Флаг, що підтверджує, що пошук був виконаний
    ) : SearchScreenState()
}