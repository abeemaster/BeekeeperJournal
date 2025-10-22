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
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.ExpenseAdapter
import com.beemaster.beekeeperjournal.databinding.FragmentExpensesBinding
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.KEY_ACTION
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.ACTION_EDIT
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.ACTION_DELETE
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.KEY_ENTRY_ID
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.KEY_ENTRY_TYPE
// ...

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
            onLongClick = { expense ->
                // ✅ ВИКЛИКАЄМО НОВИЙ УНІФІКОВАНИЙ ДІАЛОГ
                showProfitabilityActionsDialog(expense)
            }
        )
        binding.expensesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }

        // ✅ ВСТАНОВЛЮЄМО СЛУХАЧА РЕЗУЛЬТАТУ
        setupProfitabilityActionsListener()
    }

    /**
     * Встановлює слухача для обробки результату з ProfitabilityActionsDialogFragment (Редагувати/Видалити).
     */
    private fun setupProfitabilityActionsListener() {
        parentFragmentManager.setFragmentResultListener(
            ProfitabilityActionsDialogFragment.KEY_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            // ✅ ВИПРАВЛЕННЯ 1: Отримуємо Long, але одразу приводимо до Int
            val entryIdLong = bundle.getLong(KEY_ENTRY_ID)
            val entryIdInt = entryIdLong.toInt() // <-- ПРИВЕДЕННЯ ТИПУ

            val action = bundle.getString(KEY_ACTION)

            // ✅ ВИПРАВЛЕННЯ 2: Порівнюємо Int з Int
            val expenseToHandle = viewModel.expenses.value.find { it.id == entryIdInt }

            if (expenseToHandle == null) {
                Toast.makeText(requireContext(), R.string.error_entry_not_found, Toast.LENGTH_SHORT).show()
                return@setFragmentResultListener
            }

            when (action) {
                ACTION_EDIT -> {
                    // 1. РЕДАГУВАННЯ: Використовуємо існуючу логіку DialogUtils
                    // note: тут ми використовуємо expenseToHandle.hiveId, який може бути 0
                    DialogUtils.showAddExpenseDialog(
                        requireContext(),
                        viewModel,
                        hiveId = expenseToHandle.hiveId, // Передаємо hiveId (може бути 0)
                        expenseToEdit = expenseToHandle
                    )
                }
                ACTION_DELETE -> {
                    // 2. ВИДАЛЕННЯ: Використовуємо існуючу логіку DialogUtils
                    DialogUtils.showDeleteConfirmationDialog(
                        context = requireContext(),
                        titleResId = R.string.confirm_delete,
                        messageResId = R.string.delete_confirm_message,
                        onConfirm = {
                            viewModel.deleteExpense(expenseToHandle.id)
                            Toast.makeText(requireContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
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
     * Відображає BottomSheetDialogFragment для вибору дій над записом.
     */
    private fun showProfitabilityActionsDialog(expense: Expense) {
        // ✅ ВИПРАВЛЕНО: Приводимо Int до Long, щоб відповідати сигнатурі newInstance
        ProfitabilityActionsDialogFragment.newInstance(
            entryId = expense.id.toLong(), // ⬅️ ПРИВЕДЕННЯ ТИПУ ДО LONG
            entryType = Constants.TYPE_EXPENSE
        ).show(parentFragmentManager, ProfitabilityActionsDialogFragment.TAG)
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