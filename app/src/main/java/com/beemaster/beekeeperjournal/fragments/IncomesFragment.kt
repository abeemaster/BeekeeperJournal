// IncomesFragment.kt
// Фрагмент, що відображає список прибутків.
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
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import com.beemaster.beekeeperjournal.Constants // Для TYPE_INCOME
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.KEY_ACTION
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.ACTION_EDIT
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.ACTION_DELETE
import com.beemaster.beekeeperjournal.dialogs.ProfitabilityActionsDialogFragment.Companion.KEY_ENTRY_ID
import com.beemaster.beekeeperjournal.dialogs.AddIncomeDialogFragment // ✅ Додайте цей імпорт, якщо його немає
import com.beemaster.beekeeperjournal.dialogs.showDeleteConfirmationDialog


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
     * Відображає BottomSheetDialogFragment для вибору дій над записом.
     */
    private fun showProfitabilityActionsDialog(income: Income) {
        ProfitabilityActionsDialogFragment.newInstance(
            entryId = income.id.toLong(),
            entryType = Constants.TYPE_INCOME
        ).show(parentFragmentManager, ProfitabilityActionsDialogFragment.TAG)
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
            // ✅ ВИПРАВЛЕНО: Заміна DeleteConfirmationDialog.showAddIncomeDialog на AddIncomeDialogFragment.newInstance
            // hiveId = 0 означає, що прибуток не прив'язаний до конкретного вулика
            AddIncomeDialogFragment.newInstance(hiveId = 0)
                .show(childFragmentManager, AddIncomeDialogFragment.TAG)
        }
    }

    /**
     * Налаштовує RecyclerView та адаптер для відображення списку прибутків.
     * Встановлює обробник тривалого натискання для виклику діалогу редагування/видалення.
     */
    private fun setupRecyclerView() {
        incomeAdapter = IncomeAdapter(
            onClick = { /* Обробка звичайного натискання (якщо потрібна) */ },
            onLongClick = { income ->
                // ВИКЛИКАЄМО НОВИЙ УНІФІКОВАНИЙ ДІАЛОГ
                showProfitabilityActionsDialog(income)
            }
        )
        binding.incomesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeAdapter
        }

        // ВСТАНОВЛЮЄМО СЛУХАЧА РЕЗУЛЬТАТУ
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
            val entryIdLong = bundle.getLong(KEY_ENTRY_ID)
            val entryIdInt = entryIdLong.toInt() // Приведення Long до Int
            val action = bundle.getString(KEY_ACTION)
            // entryType тут має бути "income"

            // Знаходимо об'єкт Income, який потрібно редагувати/видалити.
            val incomeToHandle = viewModel.incomes.value.find { it.id == entryIdInt }

            if (incomeToHandle == null) {
                Toast.makeText(requireContext(), R.string.error_entry_not_found, Toast.LENGTH_SHORT).show()
                return@setFragmentResultListener
            }

            when (action) {
                ACTION_EDIT -> {
                    // ✅ ВИПРАВЛЕНО: Заміна DeleteConfirmationDialog.showAddIncomeDialog на AddIncomeDialogFragment.newInstance
                    // 1. РЕДАГУВАННЯ: Використовуємо новий DialogFragment
                    AddIncomeDialogFragment.newInstance(
                        hiveId = incomeToHandle.hiveId,
                        incomeToEdit = incomeToHandle
                    ).show(childFragmentManager, AddIncomeDialogFragment.TAG)
                }
                ACTION_DELETE -> {
                    // 2. ВИДАЛЕННЯ: Використовуємо існуючу логіку DeleteConfirmationDialog
                    showDeleteConfirmationDialog(
                        context = requireContext(),
                        titleResId = R.string.confirm_delete,
                        messageResId = R.string.delete_confirm_message,
                        onConfirm = {
                            viewModel.deleteIncome(incomeToHandle.id)
                            Toast.makeText(requireContext(), R.string.income_deleted, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
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
     * Очищає посилання на View Binding, щоб уникнути витоків пам'яті.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}