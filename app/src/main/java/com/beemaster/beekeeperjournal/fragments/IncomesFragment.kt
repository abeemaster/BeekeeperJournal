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

/**
 * Фрагмент, відповідальний за відображення списку всіх прибутків та загальної суми прибутку.
 * Використовує [ProfitabilityViewModel] для отримання та обробки даних.
 */
@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private var _binding: FragmentIncomesBinding? = null
    // Надає доступ до View Binding, безпечний від null після onCreateView
    private val binding get() = _binding!!

    private val viewModel: ProfitabilityViewModel by viewModels()
    private lateinit var incomeAdapter: IncomeAdapter

    /**
     * Створює і повертає ієрархію представлень для фрагмента.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomesBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Ініціалізує UI-компоненти, налаштовує RecyclerView,
     * починає спостереження за LiveData/Flow та встановлює обробники подій.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeIncomes()
        observeTotalIncome()

        binding.fabAddIncome.setOnClickListener {
            // hiveId = 0 означає, що прибуток не прив'язаний до конкретного вулика
            DialogUtils.showAddIncomeDialog(requireContext(), viewModel, hiveId = 0)
        }
    }

    /**
     * Налаштовує RecyclerView та адаптер для відображення списку прибутків.
     * Встановлює обробник тривалого натискання для виклику діалогу редагування/видалення.
     */
    private fun setupRecyclerView() {
        incomeAdapter = IncomeAdapter(
            onClick = { /* Обробка звичайного натискання (якщо потрібна) */ },
            onLongClick = { incomeEntity ->
                showEditDeleteDialog(incomeEntity)
            }
        )
        binding.incomesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeAdapter
        }
    }

    /**
     * Спостерігає за потоком [ProfitabilityViewModel.incomes] та оновлює адаптер.
     * Використовує [repeatOnLifecycle] для безпечного збору даних.
     */
    private fun observeIncomes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.incomes.collect { incomes ->
                    incomeAdapter.submitList(incomes)
                }
            }
        }
    }

    /**
     * Спостерігає за загальною сумою прибутку ([ProfitabilityViewModel.totalIncome])
     * та форматує її для відображення у відповідному TextView.
     */
    private fun observeTotalIncome() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalIncome.collect { totalIncome ->
                    // Форматуємо суму до двох знаків після коми
                    val formattedTotal = String.format(Locale.getDefault(), "%.2f", totalIncome ?: 0.0)
                    binding.totalIncomeTextView.text = getString(R.string.total_income_text, formattedTotal)
                }
            }
        }
    }

    /**
     * Відображає діалог редагування або видалення для обраного запису про прибуток.
     * @param income [IncomeEntity] запис, який потрібно редагувати або видалити.
     */
    private fun showEditDeleteDialog(income: IncomeEntity) {
        DialogUtils.showEditDeleteDialog(
            context = requireContext(),
            onEdit = {
                // Викликаємо діалог додавання/редагування, передаючи об'єкт для редагування
                DialogUtils.showAddIncomeDialog(requireContext(), viewModel, hiveId = income.hiveId, incomeToEdit = income)
            },
            onDelete = {
                // Відображаємо діалог підтвердження видалення
                DialogUtils.showDeleteConfirmationDialog(
                    context = requireContext(),
                    titleResId = R.string.confirm_delete,
                    messageResId = R.string.delete_confirm_message,
                    onConfirm = {
                        viewModel.deleteIncome(income.id)
                        // ✅ ПОКРАЩЕННЯ: Рекомендується використовувати R.string.income_deleted для консистентності UX
                        Toast.makeText(requireContext(), (R.string.note_deleted), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    /**
     * Очищає посилання на View Binding, щоб уникнути витоків пам'яті.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}