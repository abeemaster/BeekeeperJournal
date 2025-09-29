// Цей клас буде керувати даними для HiveInfoActivity.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
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

    /**
     * Приватний MutableStateFlow, що містить список нотаток.
     * Використовується для внутрішнього оновлення даних.
     */
    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())

    /**
     * Публічний StateFlow, який UI (HiveInfoActivity) може безпечно спостерігати.
     */
    val notes: StateFlow<List<NoteEntity>> = _notes.asStateFlow()

    /**
     * Завантажує та починає спостерігати за нотатками для конкретного вулика та типу запису.
     * Оновлює StateFlow [_notes] у реальному часі.
     * @param hiveId ID вулика (0 для загальних нотаток).
     * @param noteType Тип запису ("hive", "queen", "notes", "general").
     */
    fun getNotesForHive(hiveId: Int, noteType: String) {
        viewModelScope.launch {
            noteRepository.getNotesByHiveAndType(hiveId, noteType)
                .collect { notesList ->
                    _notes.value = notesList
                }
        }
    }

    /**
     * Видаляє нотатку з бази даних за її об'єктом.
     * @param note Об'єкт NoteEntity для видалення.
     */
    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note.id)
        }
    }

    /**
     * Отримує об'єкт вулика за його унікальним ID.
     * Ця функція використовується для отримання номера вулика (hiveNumber)
     * для відображення в заголовку HiveInfoActivity.
     * @param hiveId Унікальний ID вулика.
     * @return Об'єкт HiveEntity або null, якщо вулик не знайдено.
     */
    suspend fun getHiveById(hiveId: Int): HiveEntity? {
        return hiveRepository.getHiveById(hiveId)
    }

}