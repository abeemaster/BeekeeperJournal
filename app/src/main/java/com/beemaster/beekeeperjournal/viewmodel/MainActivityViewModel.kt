// Цей клас буде керувати даними для MainActivity.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.NaturalHiveNumberComparator
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
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

    // Потік даних, який отримує всі вулики, сортує їх за номером і зберігає стан.
    // Використовує StateFlow для збору даних у реальному часі.
    private val _hives = hiveRepository.getAllHivesAsFlow()
        .map { hivesList ->
            hivesList.sortedWith(compareBy(NaturalHiveNumberComparator) { it.hiveNumber })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Публічний StateFlow, який UI може спостерігати.
    val hives: StateFlow<List<HiveEntity>> = _hives

    // Функція для додавання нового вулика в базу даних.
    fun addHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.insertHive(hiveEntity)
        }
    }

    // Функція для оновлення існуючого вулика.
    fun updateHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.updateHive(hiveEntity)
        }
    }

    // Функція для видалення вулика з бази даних.
    fun deleteHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.deleteHive(hiveEntity)
        }
    }

    // Функція для додавання нової нотатки.
    fun addNote(note: NoteEntity) = viewModelScope.launch {
        noteRepository.insertNote(note)
    }

    // Функція для перевірки, чи існує вулик з певним номером.
    // Це потрібно, щоб уникнути дублювання вуликів за замовчуванням.
    suspend fun getHiveByNumber(hiveNumber: String): HiveEntity? {
        return hiveRepository.getHiveByNumber(hiveNumber)
    }

    // Функція для отримання всіх вуликів. Використовується для експорту даних.
    suspend fun getAllHivesSuspend(): List<HiveEntity> {
        return hiveRepository.getAllHives()
    }

    // Функція для отримання всіх нотаток. Використовується для експорту даних.
    suspend fun getAllNotesSuspend(): List<NoteEntity> {
        return noteRepository.getAllNotesSuspend()
    }

    // Функція для отримання всіх витрат. Використовується для експорту даних.
    suspend fun getAllExpensesSuspend(): List<ExpenseEntity> {
        return expenseRepository.getAllExpensesSuspend()
    }

    // Функція для отримання всіх прибутків. Використовується для експорту даних.
    suspend fun getAllIncomesSuspend(): List<IncomeEntity> {
        return incomeRepository.getAllIncomesSuspend()
    }

    // Функція для імпорту списку вуликів.
    fun importHives(hives: List<HiveEntity>) = viewModelScope.launch(Dispatchers.IO) {
        hiveRepository.importHives(hives)
    }

    // Функція для імпорту списку нотаток.
    fun importNotes(notes: List<NoteEntity>) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.importNotes(notes)
    }

    // Функція для імпорту списку витрат.
    fun importExpenses(expenses: List<ExpenseEntity>) = viewModelScope.launch(Dispatchers.IO) {
        expenseRepository.importExpenses(expenses)
    }

    // Функція для імпорту списку прибутків.
    fun importIncomes(incomes: List<IncomeEntity>) = viewModelScope.launch(Dispatchers.IO) {
        incomeRepository.importIncomes(incomes)
    }
}

