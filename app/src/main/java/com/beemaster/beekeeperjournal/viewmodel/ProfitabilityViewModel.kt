// ProfitabilityViewModel.kt
// ViewModel для керування даними про прибутки, витрати та рентабельність.
// Цей клас буде відповідати за бізнес-логіку екрана рентабельності,
// взаємодіючи з репозиторіями для отримання та оновлення даних.

package com.beemaster.beekeeperjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfitabilityViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    // потоки для спостереження за даними
    val incomes: StateFlow<List<IncomeEntity>> =
        incomeRepository.getAllIncomes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenses: StateFlow<List<ExpenseEntity>> =
        expenseRepository.getAllExpenses().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalIncome: StateFlow<Double?> =
        incomeRepository.getTotalIncomeFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalExpense: StateFlow<Double?> =
        expenseRepository.getTotalExpenseFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun insertIncome(income: IncomeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            incomeRepository.insertIncome(income)
        }
    }

    fun updateIncome(income: IncomeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            incomeRepository.updateIncome(income)
        }
    }

    fun deleteIncome(incomeId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            incomeRepository.deleteIncome(incomeId)
        }
    }

    fun insertExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.updateExpense(expense)
        }
    }

    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.deleteExpense(expenseId)
        }
    }

    val profitability: StateFlow<Double> =
        combine(totalIncome, totalExpense) { income, expense ->
            (income ?: 0.0) - (expense ?: 0.0)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

}