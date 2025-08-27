// Цей клас буде керувати даними для MainActivity.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.HiveEntity
import com.beemaster.beekeeperjournal.db.NaturalHiveNumberComparator
import com.beemaster.beekeeperjournal.repository.HiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val hiveRepository: HiveRepository
) : ViewModel() {

    private val _hives = hiveRepository.getAllHivesAsFlow()
        .map { hivesList ->
            // ✅ Застосовуємо сортування перед тим, як оновити StateFlow
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

    // ✅ ДОДАНО: Метод для оновлення вулика
    fun updateHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.updateHive(hiveEntity)
        }
    }

    // ✅ ДОДАНО: Метод для видалення вулика
    fun deleteHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.deleteHive(hiveEntity)
        }
    }
}