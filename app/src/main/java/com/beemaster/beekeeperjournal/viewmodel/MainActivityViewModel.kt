// Цей клас буде керувати даними для MainActivity.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.mappers.toIncomeEntity
import com.beemaster.beekeeperjournal.models.BackupData
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.repository.YearRepository
import com.beemaster.beekeeperjournal.utils.BackupPrefsManager
import com.beemaster.beekeeperjournal.utils.NaturalHiveNumberComparator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val incomeRepository: IncomeRepository,
    private val yearRepository: YearRepository,
    private val backupPrefsManager: BackupPrefsManager
) : ViewModel(), BackupDataSource {

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

    // --------------------------------------------------------------------
    // НОВІ МЕТОДИ ДЛЯ РЕЗЕРВНОГО КОПІЮВАННЯ (ВИКЛИКАЮТЬСЯ З BackupManager)
    // --------------------------------------------------------------------

    /**
     * Асинхронно збирає всі дані з бази даних (вулики, нотатки, витрати, прибутки)
     * та повертає їх у вигляді об'єкта [BackupData] для серіалізації.
     * ВИРІШУЄ Unresolved reference 'exportAllData'
     */
    @Suppress("unused")
    suspend fun exportAllData(): BackupData { // Насправді функцію викристовує BackupManager.kt для збору даних перед експортом.
        return withContext(Dispatchers.IO) {
            // Викликаємо існуючі методи ViewModel для агрегації
            val hives = getAllHivesSuspend()
            val notes = getAllNotesSuspend()
            val expenses = getAllExpensesSuspend()
            val incomes = getAllIncomesSuspend()
            val years = getAllYearsSuspend()

            // Конвертуємо Income (модель) у IncomeEntity (для BackupData),
            // оскільки BackupData.kt очікує List<IncomeEntity>
            val incomeEntities = incomes.map { it.toIncomeEntity() }

            BackupData(
                hives = hives,
                notes = notes,
                expenses = expenses,
                incomes = incomeEntities,
                years = years
            )
        }
    }

    /**
     * Реалізує логіку перевірки змін даних.
     * Повертає true, якщо дані в БД змінилися пізніше, ніж був зроблений останній бекап (або якщо бекапу ще не було).
     */
    override fun hasDataChanged(): Boolean {
        val lastModified = backupPrefsManager.getLastDataModifiedTime()
        val lastBackup = backupPrefsManager.getLastBackupTime()

        // Бекап потрібен, якщо дані змінювалися пізніше, ніж був зроблений останній бекап.
        // Також бекап потрібен, якщо lastBackup == 0L (бекап ніколи не робився).
        return lastModified > lastBackup
    }

    // --- Реалізація для РОКІВ (BeekeepingYear) ---

    /**
     * Отримує всі роки для експорту.
     */
    override suspend fun getAllYearsSuspend(): List<BeekeepingYear> {
        return yearRepository.getAllYearsStatic()
    }

    /**
     * Імпортує роки з резервної копії.
     */
    override suspend fun importYears(years: List<BeekeepingYear>) = withContext(Dispatchers.IO) {
        yearRepository.importYears(years)
        backupPrefsManager.updateLastDataModifiedTime()
    }

    // --------------------------------------------------------------------
    // ІСНУЮЧІ МЕТОДИ
    // --------------------------------------------------------------------

    /**
     * Додає новий об'єкт HiveEntity без попередніх перевірок.
     * @param hiveEntity Об'єкт вулику для вставки.
     */
    fun addHive(hiveEntity: HiveEntity) {
        viewModelScope.launch {
            hiveRepository.insertHive(hiveEntity)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    /**
     * Виконує повну бізнес-логіку додавання вулика:
     * ... (тут була логіка addNewHive) ...
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
        backupPrefsManager.updateLastDataModifiedTime()
        _hiveEventChannel.send(HiveAddResult.SUCCESS)
    }

    /**
     * Оновлює номер вулика з перевіркою на конфлікт номерів.
     * @param hiveId ID вулика, який оновлюється.
     * @param newNumber Новий номер.
     */
    fun updateHiveNumberWithValidation(hiveId: Long, newNumber: String) = viewModelScope.launch {
        // 1. Перевірка існування (чи існує ВЖЕ інший вулик з цим номером)
        val existingHive = hiveRepository.getHiveByNumber(newNumber)

        if (existingHive != null && existingHive.id.toLong() != hiveId) {
            _hiveEventChannel.send(HiveAddResult.EXISTS)
            return@launch
        }

        // 2. Оновлення, якщо конфлікту немає
        hiveRepository.updateHiveNumber(hiveId, newNumber)
        // _hiveEventChannel.send(HiveAddResult.SUCCESS) // Надсилаємо успіх
    }

    /**
     * Видаляє об'єкт HiveEntity з бази даних.
     * @param hiveEntity Об'єкт вулику для видалення.
     */
    fun deleteHive(hiveId: Long) { // Змінили параметр з HiveEntity на Long
        viewModelScope.launch {

            // 1. Знаходимо HiveEntity за ID (вимагає hiveRepository.getHiveById)
            val hiveEntity = hiveRepository.getHiveById(hiveId)

            // 2. Якщо об'єкт знайдено, викликаємо функцію видалення у репозиторії,
            // яка, у свою чергу, викликає Room @Delete.
            hiveEntity?.let {
                hiveRepository.deleteHive(it)
                backupPrefsManager.updateLastDataModifiedTime()
            }
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
     * Припускаємо, що метод у репозиторії називається getAllHives()
     */
    override suspend fun getAllHivesSuspend(): List<HiveEntity> {
        return hiveRepository.getAllHives()

    }

    /**
     * Отримує всі об'єкти Note. Використовується для експорту даних.
     * Бере одноразовий знімок даних з потоку Flow.
     * @return Список усіх Note.
     * Видалено зайвий маппер, оскільки repo, ймовірно, повертає List<Note>.
     */
    override suspend fun getAllNotesSuspend(): List<Note> {
        // ВИПРАВЛЕНО: Використовуємо спеціальний метод для бекапу, який повертає List<Note>
        return noteRepository.getAllNotesForExportSuspend()
    }
    /**
     * Отримує всі об'єкти Expense. Використовується для експорту даних.
     * @return Список усіх Expense.
     */
    override suspend fun getAllExpensesSuspend(): List<Expense> {
        return expenseRepository.getAllExpensesSuspend()

    }

    /**
     * Отримує всі об'єкти Income. Використовується для експорту даних.
     * @return Список усіх Income.
     */
    override suspend fun getAllIncomesSuspend(): List<Income> {
        return incomeRepository.getAllIncomesSuspend()
    }

    /**
     * Імпортує список HiveEntity в базу даних.
     * @param hives Список об'єктів для імпорту.
     */
    override suspend fun importHives(hives: List<HiveEntity>) = withContext(Dispatchers.IO) {
        hiveRepository.importHives(hives)
        backupPrefsManager.updateLastDataModifiedTime()
    }

    /**
     * Імпортує список Note в базу даних.
     * @param notes Список об'єктів для імпорту.
     */
    override fun importNotes(notes: List<Note>) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.importNotes(notes)
        backupPrefsManager.updateLastDataModifiedTime()
    }

    /**
     * Імпортує список Expense в базу даних.
     * @param expenses Список об'єктів для імпорту.
     */
    override fun importExpenses(expenses: List<Expense>) = viewModelScope.launch(Dispatchers.IO) {
        expenseRepository.importExpenses(expenses)
        backupPrefsManager.updateLastDataModifiedTime()
    }

    /**
     * Імпортує список Income в базу даних.
     * @param incomes Список об'єктів для імпорту.
     */
    override fun importIncomes(incomes: List<Income>) = viewModelScope.launch(Dispatchers.IO) {
        incomeRepository.importIncomes(incomes)
        backupPrefsManager.updateLastDataModifiedTime()
    }

    fun updateHivePrimaryColor(hiveId: Long, @ColorInt color: Int) {
        viewModelScope.launch {
            hiveRepository.updatePrimaryColor(hiveId, color)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    fun updateHiveSecondaryColor(hiveId: Long, @ColorInt color: Int) {
        viewModelScope.launch {
            hiveRepository.updateSecondaryColor(hiveId, color)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }
}