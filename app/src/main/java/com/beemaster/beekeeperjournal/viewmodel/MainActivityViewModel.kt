// Цей клас буде керувати даними для MainActivity.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import com.beemaster.beekeeperjournal.db.entity.NaturalHiveNumberComparator
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
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
    private val noteRepository: NoteRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
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

    fun addNote(note: NoteEntity) = viewModelScope.launch {
        noteRepository.insertNote(note)
    }

    suspend fun getAllHivesSuspend(): List<HiveEntity> {
        return hiveRepository.getAllHivesSuspend()
    }

    suspend fun getAllNotesSuspend(): List<NoteEntity> {
        return noteRepository.getAllNotesSuspend()
    }

    suspend fun getAllExpensesSuspend(): List<ExpenseEntity> {
        return expenseRepository.getAllExpensesSuspend()
    }

    suspend fun getAllIncomesSuspend(): List<IncomeEntity> {
        return incomeRepository.getAllIncomesSuspend()
    }

    fun importHives(hives: List<HiveEntity>) = viewModelScope.launch(Dispatchers.IO) {
        hiveRepository.importHives(hives)
    }

    fun importNotes(notes: List<NoteEntity>) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.importNotes(notes)
    }

    fun importExpenses(expenses: List<ExpenseEntity>) = viewModelScope.launch(Dispatchers.IO) {
        expenseRepository.importExpenses(expenses)
    }

    fun importIncomes(incomes: List<IncomeEntity>) = viewModelScope.launch(Dispatchers.IO) {
        incomeRepository.importIncomes(incomes)
    }
}
