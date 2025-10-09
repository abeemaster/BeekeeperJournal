// У файлі HiveInfoViewModel.kt

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.models.NoteDisplayModel
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.repository.HiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для екрана HiveInfoActivity.
 * Відповідає за отримання та керування нотатками для конкретного вулика
 * або загальних записів.
 */
@HiltViewModel
class HiveInfoViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val hiveRepository: HiveRepository
) : ViewModel() {

    // ------------------------------------
    // FLOWS ДЛЯ UI
    // ------------------------------------

    /**
     * MutableStateFlow для зберігання списку нотаток ([Note]).
     * Використовується для оновлення RecyclerView в Activity/Fragment.
     */
    private val _notes = MutableStateFlow<List<NoteDisplayModel>>(emptyList())

    /**
     * StateFlow, який UI може безпечно збирати (collect) для відображення нотаток.
     */
    val notes: StateFlow<List<NoteDisplayModel>> = _notes.asStateFlow()

    // ------------------------------------
    // ФУНКЦІЇ ОТРИМАННЯ ДАНИХ
    // ------------------------------------

    /**
     * Отримує об'єкт вулика за його унікальним ID.
     * Використовується для завантаження номера вулика (hiveNumber) на початку активіті.
     * @param hiveId ID вулика.
     * @return Об'єкт HiveEntity.
     */
    suspend fun getHiveById(hiveId: Int) = hiveRepository.getHiveById(hiveId)

    /**
     * Запускає спостереження за нотатками для конкретного вулика та типу запису.
     * Результат оновлює [_notes] через Flow.
     * @param hiveId ID вулика (0 для загальних записів).
     * @param noteType Тип запису ("hive", "queen", "notes", "general").
     */
    fun getNotesForHive(hiveId: Int, noteType: String) {
        viewModelScope.launch {
            // ✅ ВИПРАВЛЕНО: Використовуємо нову функцію репозиторію, яка повертає NoteDisplayModel
            noteRepository.getNotesForHiveDisplay(hiveId, noteType)
                .collect { notesList ->
                    _notes.value = notesList // Оновлюємо StateFlow
                }
        }
    }

    // ------------------------------------
    // ФУНКЦІЇ ЗМІНИ ДАНИХ (CRUD)
    // ------------------------------------

    /**
     * Видаляє нотатку з бази даних.
     * @param note Доменна модель [Note], яку потрібно видалити.
     *
     * ⚠️ ПРИМІТКА: Репозиторій видаляє за ID. Якщо NoteRepository.deleteNote()
     * очікує лише ID, тоді цей метод коректний.
     */
    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            // Викликаємо функцію репозиторію для видалення за ID нотатки
            noteRepository.deleteNote(noteId)
        }
    }
}