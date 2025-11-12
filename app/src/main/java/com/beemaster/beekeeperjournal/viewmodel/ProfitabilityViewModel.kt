// ProfitabilityViewModel.kt
// ViewModel для керування даними про прибутки, витрати та рентабельність.
// Цей клас буде відповідати за бізнес-логіку екрана рентабельності,
// взаємодіючи з репозиторіями для отримання та оновлення даних.


package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
import com.beemaster.beekeeperjournal.utils.BackupPrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для екрана рентабельності.
 * Відповідає за отримання, керування та обчислення фінансових даних
 * (прибутки та витрати) з репозиторіїв.
 */
@HiltViewModel
class ProfitabilityViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val backupPrefsManager: BackupPrefsManager
) : ViewModel() {

    // ----------------------
    // РЕАКТИВНІ ПОТОКИ ДАНИХ
    // ----------------------

    /**
     * Потік, що містить список усіх записів про прибутки.
     */
    val incomes: StateFlow<List<Income>> = // ЗМІНА ТИПУ НА Income
        incomeRepository.getAllIncomes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Потік, що містить список усіх записів про витрати.
     */
    val expenses: StateFlow<List<Expense>> =
        expenseRepository.getAllExpenses().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Потік, що містить загальну суму прибутків.
     */
    val totalIncome: StateFlow<Double?> =
        incomeRepository.getTotalIncomeFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Потік, що містить загальну суму витрат.
     */
    val totalExpense: StateFlow<Double?> =
        expenseRepository.getTotalExpenseFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Потік, що обчислює рентабельність як різницю між загальним прибутком та загальними витратами.
     * Обчислення відбувається автоматично при зміні будь-якого з вихідних потоків (combine).
     */
    val profitability: StateFlow<Double> =
        combine(totalIncome, totalExpense) { income, expense ->
            (income ?: 0.0) - (expense ?: 0.0)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // ----------------------
    // ФУНКЦІЇ ДЛЯ УПРАВЛІННЯ ПРИБУТКАМИ (CRUD)
    // ----------------------

    /**
     * Вставляє новий запис про прибуток.
     * Виконується в фоновому потоці Dispatchers.IO.
     * @param income Об'єкт Income для вставки.
     */
    fun insertIncome(income: Income) {
        viewModelScope.launch(Dispatchers.IO) {
            incomeRepository.insertIncome(income)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    /**
     * Оновлює існуючий запис про прибуток.
     * Виконується в фоновому потоці Dispatchers.IO.
     * @param income Об'єкт Income для оновлення.
     */
    fun updateIncome(income: Income) {
        viewModelScope.launch(Dispatchers.IO) {
            incomeRepository.updateIncome(income)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    /**
     * Видаляє запис про прибуток за його ID.
     * Виконується в фоновому потоці Dispatchers.IO.
     * @param incomeId ID запису про прибуток для видалення.
     */
    fun deleteIncome(incomeId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            incomeRepository.deleteIncome(incomeId)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    // ----------------------
    // ФУНКЦІЇ ДЛЯ УПРАВЛІННЯ ВИТРАТАМИ (CRUD)
    // ----------------------

    /**
     * Вставляє новий запис про витрати.
     * Виконується в фоновому потоці Dispatchers.IO.
     * @param expense Об'єкт Expense для вставки.
     */
    fun insertExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.insertExpense(expense)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    /**
     * Оновлює існуючий запис про витрати.
     * Виконується в фоновому потоці Dispatchers.IO.
     * @param expense Об'єкт Expense для оновлення.
     */
    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.updateExpense(expense)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }

    /**
     * Видаляє запис про витрати за його ID.
     * Виконується в фоновому потоці Dispatchers.IO.
     * @param expenseId ID запису про витрати для видалення.
     */
    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.deleteExpense(expenseId)
            backupPrefsManager.updateLastDataModifiedTime()
        }
    }
}
