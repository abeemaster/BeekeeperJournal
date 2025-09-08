// Цей клас буде керувати даними для MainActivity.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.HiveEntity
import com.beemaster.beekeeperjournal.db.NaturalHiveNumberComparator
import com.beemaster.beekeeperjournal.db.NoteEntity
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val hiveRepository: HiveRepository,
    private val noteRepository: NoteRepository, // ✅ Додано залежність NoteRepository
    private val repository: NoteRepository
) : ViewModel() {

    private val _hives = hiveRepository.getAllHivesAsFlow()
        .map { hivesList ->
            hivesList.sortedWith(compareBy(NaturalHiveNumberComparator) { it.hiveNumber })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hives: StateFlow<List<HiveEntity>> = _hives

    fun addHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.insertHive(hiveEntity)
        }
    }

    fun updateHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.updateHive(hiveEntity)
        }
    }

    fun deleteHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.deleteHive(hiveEntity)
        }
    }
    // ✅ Виправлено виклик, щоб використовувати ін'єктований noteRepository
    fun addNote(note: NoteEntity) = viewModelScope.launch {
        noteRepository.insertNote(note)
    }

    // ✅ Додаємо методи для експорту даних
    suspend fun getAllHivesSuspend(): List<HiveEntity> {
        return repository.getAllHivesSuspend()
    }

    suspend fun getAllNotesSuspend(): List<NoteEntity> {
        return repository.getAllNotesSuspend()
    }

    // ✅ Додаємо методи для імпорту даних
    fun importHives(hives: List<HiveEntity>) = viewModelScope.launch(Dispatchers.IO) {
        repository.importHives(hives)
    }

    fun importNotes(notes: List<NoteEntity>) = viewModelScope.launch(Dispatchers.IO) {
        repository.importNotes(notes)
    }
}