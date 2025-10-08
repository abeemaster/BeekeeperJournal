// Цей клас буде керувати даними для MainActivity.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.utils.NaturalHiveNumberComparator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Клас-перерахування для кодування результатів операцій додавання вулика.
 * Використовується для надсилання одноразових подій (Toast) назад в Activity.
 */
enum class HiveAddResult {
    SUCCESS,        // Успішно додано
    EXISTS,         // Вулик з таким номером вже існує
    LIMIT_REACHED   // Досягнуто максимального ліміту
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val hiveRepository: HiveRepository,
    private val noteRepository: NoteRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) : ViewModel() {

    private val MAX_HIVES_LIMIT = 100 // Максимальний ліміт вуликів

    // Channel для надсилання одноразових подій, які обробляються Activity.
    private val _hiveEventChannel = Channel<HiveAddResult>()
    val hiveEvents = _hiveEventChannel.receiveAsFlow() // Activity буде спостерігати за цим Flow

    // Потік даних, який отримує, сортує (за номером) та зберігає стан усіх вуликів.
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

    /**
     * Додає новий об'єкт HiveEntity без попередніх перевірок.
     * Використовується, наприклад, при імпорті даних або створенні вулика за замовчуванням.
     * @param hiveEntity Об'єкт вулику для вставки.
     */
    fun addHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.insertHive(hiveEntity)
        }
    }

    /**
     * Виконує повну бізнес-логіку додавання вулика:
     * 1. Перевіряє ліміт MAX_HIVES_LIMIT.
     * 2. Перевіряє унікальність номера.
     * 3. Додає вулик та надсилає подію HiveAddResult.
     * @param newHiveEntity Об'єкт вулику для додавання.
     */
    fun addNewHive(newHiveEntity: HiveEntity) = viewModelScope.launch {
        // 1. Перевірка ліміту
        if (hives.value.size >= MAX_HIVES_LIMIT) {
            _hiveEventChannel.send(HiveAddResult.LIMIT_REACHED)
            return@launch
        }

        // 2. Перевірка існування
        val existingHive = hiveRepository.getHiveByNumber(newHiveEntity.hiveNumber)
        if (existingHive != null) {
            _hiveEventChannel.send(HiveAddResult.EXISTS)
            return@launch
        }

        // 3. Додавання
        hiveRepository.insertHive(newHiveEntity)
        _hiveEventChannel.send(HiveAddResult.SUCCESS)
    }

    /**
     * Оновлює існуючий об'єкт HiveEntity.
     * @param hiveEntity Оновлений об'єкт вулику.
     */
    fun updateHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.updateHive(hiveEntity)
        }
    }

    /**
     * Видаляє об'єкт HiveEntity з бази даних.
     * @param hiveEntity Об'єкт вулику для видалення.
     */
    fun deleteHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.deleteHive(hiveEntity)
        }
    }

    /**
     * Отримує об'єкт HiveEntity за його номером.
     * @param hiveNumber Номер вулику для пошуку.
     * @return Об'єкт HiveEntity або null.
     */
    suspend fun getHiveByNumber(hiveNumber: String): HiveEntity? {
        return hiveRepository.getHiveByNumber(hiveNumber)
    }

    /**
     * Отримує всі об'єкти HiveEntity. Використовується для експорту даних.
     * @return Список усіх HiveEntity.
     */
    suspend fun getAllHivesSuspend(): List<HiveEntity> {
        return hiveRepository.getAllHives()
    }

    /**
     * Отримує всі об'єкти NoteEntity. Використовується для експорту даних.
     * Бере одноразовий знімок даних з потоку Flow.
     * @return Список усіх NoteEntity.
     */
    suspend fun getAllNotesSuspend(): List<Note> {
        return noteRepository.getAllNotes().first()
    }

    /**
     * Отримує всі об'єкти ExpenseEntity. Використовується для експорту даних.
     * @return Список усіх ExpenseEntity.
     */
    suspend fun getAllExpensesSuspend(): List<Expense> {
        return expenseRepository.getAllExpensesSuspend()
    }

    /**
     * Отримує всі об'єкти IncomeEntity. Використовується для експорту даних.
     * @return Список усіх IncomeEntity.
     */
    suspend fun getAllIncomesSuspend(): List<Income> {
        return incomeRepository.getAllIncomesSuspend()
    }

    // Функції імпорту...

    /**
     * Імпортує список HiveEntity в базу даних.
     * @param hives Список об'єктів для імпорту.
     */
    suspend fun importHives(hives: List<HiveEntity>) = withContext(Dispatchers.IO) {
        hiveRepository.importHives(hives)
    }

    /**
     * Імпортує список NoteEntity в базу даних.
     * @param notes Список об'єктів для імпорту.
     */
    fun importNotes(notes: List<Note>) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.importNotes(notes)
    }

    /**
     * Імпортує список ExpenseEntity в базу даних.
     * @param expenses Список об'єктів для імпорту.
     */
    fun importExpenses(expenses: List<Expense>) = viewModelScope.launch(Dispatchers.IO) {
        expenseRepository.importExpenses(expenses)
    }

    /**
     * Імпортує список IncomeEntity в базу даних.
     * @param incomes Список об'єктів для імпорту.
     */
    fun importIncomes(incomes: List<Income>) = viewModelScope.launch(Dispatchers.IO) {
        incomeRepository.importIncomes(incomes)
    }
}