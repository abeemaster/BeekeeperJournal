// ExpensesFragment.kt
// Фрагмент, що відображає список витрат.

package com.beemaster.beekeeperjournal.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.ExpenseAdapter
import com.beemaster.beekeeperjournal.databinding.FragmentExpensesBinding
// import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity // ❌ ВИДАЛЕНО
import com.beemaster.beekeeperjournal.models.Expense // ✅ ДОДАНО: Використовуємо бізнес-модель
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Фрагмент для відображення списку витрат та загальної суми витрат.
 * Використовує Hilt для ін'єкції ViewModel.
 */
@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    // Надає доступ до View Binding, безпечний від null після onCreateView
    private val binding get() = _binding!!

    // Ініціалізація ViewModel через viewModels()
    private val viewModel: ProfitabilityViewModel by viewModels()
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeExpenses()
        observeTotalExpense()

        binding.fabAddExpense.setOnClickListener {
            // hiveId = 0 означає, що витрата не прив'язана до конкретного вулика
            DialogUtils.showAddExpenseDialog(requireContext(), viewModel, hiveId = 0)
        }
    }

    /**
     * Налаштовує RecyclerView та адаптер для відображення списку витрат.
     */
    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onClick = { /* Можна додати обробку звичайного натискання, якщо потрібно */ },
            onLongClick = { expense -> // ✅ Змінено тип аргументу на Expense
                showEditDeleteDialog(expense)
            }
        )
        binding.expensesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    /**
     * Спостерігає за списком витрат з ViewModel та оновлює RecyclerView.
     */
    private fun observeExpenses() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // expenses тепер повертає List<Expense>, що відповідає адаптеру
                viewModel.expenses.collect { expenses ->
                    expenseAdapter.submitList(expenses)
                }
            }
        }
    }

    /**
     * Спостерігає за загальною сумою витрат та оновлює текстове поле.
     */
    private fun observeTotalExpense() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalExpense.collect { totalExpense ->
                    val formattedTotal = String.format(Locale.getDefault(), "%.2f", totalExpense ?: 0.0)
                    binding.totalExpensesTextView.text = getString(R.string.total_expenses_text, formattedTotal)
                }
            }
        }
    }

    /**
     * Відображає діалог редагування/видалення при тривалому натисканні на елемент.
     */
    private fun showEditDeleteDialog(expense: Expense) { // ✅ Змінено тип аргументу на Expense
        DialogUtils.showEditDeleteDialog(
            context = requireContext(),
            onEdit = {
                // ✅ ВИПРАВЛЕНО: Тепер передаємо об'єкт Expense у DialogUtils
                DialogUtils.showAddExpenseDialog(requireContext(), viewModel, hiveId = expense.hiveId, expenseToEdit = expense)
            },
            onDelete = {
                DialogUtils.showDeleteConfirmationDialog(
                    context = requireContext(),
                    titleResId = R.string.confirm_delete,
                    messageResId = R.string.delete_confirm_message,
                    onConfirm = {
                        viewModel.deleteExpense(expense.id)
                        // ✅ ПОКРАЩЕННЯ: Використовуйте R.string.expense_deleted (якщо створено)
                        // Залишив R.string.note_deleted як приклад, але краще використовувати специфічний рядок.
                        Toast.makeText(requireContext(), getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Звільняємо посилання на binding, щоб уникнути витоків пам'яті
        _binding = null
    }
}