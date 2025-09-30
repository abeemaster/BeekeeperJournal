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
import kotlinx.coroutines.flow.first // ✅ НЕОБХІДНИЙ ІМПОРТ
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

    // Функції CRUD для HiveEntity...

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

    // Функція для додавання нової нотатки.
    fun addNote(note: NoteEntity) = viewModelScope.launch {
        noteRepository.insertNote(note)
    }

    // Функція для перевірки, чи існує вулик з певним номером.
    suspend fun getHiveByNumber(hiveNumber: String): HiveEntity? {
        return hiveRepository.getHiveByNumber(hiveNumber)
    }

    // Функція для отримання всіх вуликів. Використовується для експорту даних.
    suspend fun getAllHivesSuspend(): List<HiveEntity> {
        return hiveRepository.getAllHives()
    }

    /**
     * ✅ ВИПРАВЛЕНО: Отримує всі нотатки для експорту.
     * Використовує noteRepository.getAllNotes() (який повертає Flow) та .first()
     * для отримання одноразового знімка даних.
     */
    suspend fun getAllNotesSuspend(): List<NoteEntity> {
        // Ми викликаємо Flow-метод і беремо його перший (поточний) результат
        return noteRepository.getAllNotes().first()
    }

    // Функція для отримання всіх витрат. Використовується для експорту даних.
    // ПРИПУЩЕННЯ: ExpenseRepository також має бути оновлений для використання .first()
    suspend fun getAllExpensesSuspend(): List<ExpenseEntity> {
        return expenseRepository.getAllExpensesSuspend()
    }

    // Функція для отримання всіх прибутків. Використовується для експорту даних.
    // ПРИПУЩЕННЯ: IncomeRepository також має бути оновлений для використання .first()
    suspend fun getAllIncomesSuspend(): List<IncomeEntity> {
        return incomeRepository.getAllIncomesSuspend()
    }

    // Функції імпорту...

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
