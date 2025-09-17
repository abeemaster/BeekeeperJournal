// IncomesFragment.kt
// Фрагмент, що відображає список прибутків.

package com.beemaster.beekeeperjournal.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.IncomeAdapter
import com.beemaster.beekeeperjournal.databinding.FragmentIncomesBinding
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private var _binding: FragmentIncomesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfitabilityViewModel by viewModels() // Ініціалізуємо ViewModel
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

        // ✅ ПОМИЛКА ВИПРАВЛЕНА: Передаємо hiveId = 0 для загальних прибутків
        binding.fabAddIncome.setOnClickListener {
            DialogUtils.showAddIncomeDialog(requireContext(), viewModel, 0)
        }
    }

    private fun setupRecyclerView() {
        // ✅ ПОМИЛКА ВИПРАВЛЕНА: Конструктор адаптера більше не потребує списку
        incomeAdapter = IncomeAdapter { incomeEntity ->
            // Обробник натискання на елемент списку
        }
        binding.incomesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeAdapter
        }
    }

    private fun observeIncomes() {
        lifecycleScope.launch {
            viewModel.incomes.collect { incomes ->
                // ✅ ПОМИЛКА ВИПРАВЛЕНА: Використовуємо submitList для оновлення даних в адаптері
                incomeAdapter.submitList(incomes)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
