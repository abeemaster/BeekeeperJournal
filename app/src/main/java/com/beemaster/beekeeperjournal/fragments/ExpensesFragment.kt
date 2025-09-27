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
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
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
            DialogUtils.showAddExpenseDialog(requireContext(), viewModel, hiveId = 0)
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onClick = { /* Можна додати обробку звичайного натискання, якщо потрібно */ },
            onLongClick = { expenseEntity ->
                showEditDeleteDialog(expenseEntity)
            }
        )
        binding.expensesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun observeExpenses() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expenses.collect { expenses ->
                    expenseAdapter.submitList(expenses)
                }
            }
        }
    }

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

    private fun showEditDeleteDialog(expense: ExpenseEntity) {
        DialogUtils.showEditDeleteDialog(
            context = requireContext(),
            onEdit = {
                DialogUtils.showAddExpenseDialog(requireContext(), viewModel, hiveId = expense.hiveId, expenseToEdit = expense)
            },
            onDelete = {
                DialogUtils.showDeleteConfirmationDialog(
                    context = requireContext(),
                    titleResId = R.string.confirm_delete, // Або R.string.delete_expense_title
                    messageResId = R.string.delete_confirm_message, // Або R.string.delete_expense_message
                    onConfirm = {
                        viewModel.deleteExpense(expense.id)
                        // Toast.makeText(requireContext(), (R.string.note_deleted), Toast.LENGTH_SHORT).show()
                        Toast.makeText(requireContext(), getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}