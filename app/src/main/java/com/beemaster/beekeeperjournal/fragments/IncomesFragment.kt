// IncomesFragment.kt
// Фрагмент, що відображає список прибутків.

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
import com.beemaster.beekeeperjournal.adapters.IncomeAdapter
import com.beemaster.beekeeperjournal.databinding.FragmentIncomesBinding
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private var _binding: FragmentIncomesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfitabilityViewModel by viewModels()
    private lateinit var incomeAdapter: IncomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeIncomes()
        observeTotalIncome()

        binding.fabAddIncome.setOnClickListener {
            // ✅ ВИПРАВЛЕНО: Передаємо `null` для нового запису
            DialogUtils.showAddIncomeDialog(requireContext(), viewModel, hiveId = 0)
        }
    }

    private fun setupRecyclerView() {
        // ✅ ОНОВЛЕНО: Передаємо обробники для обох натискань в адаптер
        incomeAdapter = IncomeAdapter(
            onClick = { /* Можна додати обробку звичайного натискання, якщо потрібно */ },
            onLongClick = { incomeEntity ->
                showEditDeleteDialog(incomeEntity)
            }
        )
        binding.incomesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeAdapter
        }
    }

    private fun observeIncomes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.incomes.collect { incomes ->
                    incomeAdapter.submitList(incomes)
                }
            }
        }
    }

    private fun observeTotalIncome() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalIncome.collect { totalIncome ->
                    val formattedTotal = String.format(Locale.getDefault(), "%.2f", totalIncome ?: 0.0)
                    binding.totalIncomeTextView.text = getString(R.string.total_income_text, formattedTotal)
                }
            }
        }
    }

    // ✅ НОВИЙ МЕТОД: Для відображення діалогу редагування/видалення
    private fun showEditDeleteDialog(income: IncomeEntity) {
        DialogUtils.showEditDeleteDialog(
            context = requireContext(),
            onEdit = {
                DialogUtils.showAddIncomeDialog(requireContext(), viewModel, hiveId = income.hiveId, incomeToEdit = income)
            },
            onDelete = {
                DialogUtils.showDeleteConfirmationDialog(
                    context = requireContext(),
                    titleResId = R.string.confirm_delete, // "Видалити запис?"
                    messageResId = R.string.delete_confirm_message, // "Ви впевнені, що хочете видалити...
                    onConfirm = {
                        viewModel.deleteIncome(income.id)
                        // Toast.makeText(requireContext(), (R.string.note_deleted), Toast.LENGTH_SHORT).show()
                        Toast.makeText(requireContext(), (R.string.note_deleted), Toast.LENGTH_SHORT).show()
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

