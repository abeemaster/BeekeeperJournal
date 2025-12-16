// dialogs/BeekeeperYearDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.BeekeepingYearAdapter
import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear
import com.beemaster.beekeeperjournal.viewmodel.BeekeepingYearViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * BottomSheetDialogFragment, який відображає список усіх доступних пасічних років
 * та дозволяє користувачеві перемикати активний рік або додавати новий рік.
 * * Використовує [BeekeepingYearViewModel] для взаємодії з даними років.
 */
@AndroidEntryPoint
class BeekeeperYearDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: BeekeepingYearViewModel by activityViewModels()
    private lateinit var yearsRecyclerView: RecyclerView
    private lateinit var addYearButton: Button
    private lateinit var currentYearTextView: TextView
    private lateinit var yearAdapter: BeekeepingYearAdapter


    // Властивість для зберігання імені поточного активного року, щоб використовувати його для розрахунку наступного року.
    private var currentActiveYearName: String? = null

    /**
     * Створює та повертає ієрархію представлень (View) для діалогового вікна.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Використовуємо наданий вами layout
        return inflater.inflate(R.layout.beekeeper_year_dialog, container, false)
    }

    /**
     * Викликається після створення View, налаштовує UI-компоненти та прив'язує адаптер.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapter() // ВИПРАВЛЕНО: Один виклик для ініціалізації адаптера
        setupListeners()
        observeViewModel()
        setupYearActionListener()
    }

    /**
     * Ініціалізує посилання на View-елементи з layout.
     * @param view Кореневе View діалогу.
     */
    private fun initViews(view: View) {
        yearsRecyclerView = view.findViewById(R.id.yearsRecyclerView)
        addYearButton = view.findViewById(R.id.addYearButton)
        currentYearTextView = view.findViewById(R.id.currentYearTextView)

        // Встановлюємо LayoutManager, якщо він не встановлений у XML
        if (yearsRecyclerView.layoutManager == null) {
            yearsRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Налаштовує обробники подій для UI-елементів.
     * Містить **виправлену** логіку додавання нового пасічного року,
     * який тепер розраховується як *наступний* рік відносно активного.
     */
    private fun setupListeners() {
        addYearButton.setOnClickListener {
            // 1. Отримуємо рік, наступний за поточним активним.
            val nextYearInt = try {
                // Намагаємося перетворити ім'я активного року на число та додати 1
                val currentYear = currentActiveYearName?.toInt()
                currentYear?.plus(1) ?: (Calendar.getInstance().get(Calendar.YEAR) + 1)
            } catch (_: NumberFormatException) {
                // Якщо ім'я активного року не є числом, використовуємо поточний календарний рік + 1
                Calendar.getInstance().get(Calendar.YEAR) + 1
            }

            val nextYearName = nextYearInt.toString()

            // 2. Встановлюємо дату початку нового року (1 січня наступного року)
            val nextYearStartDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, nextYearInt)
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                // Обнуляємо час, щоб це був початок дня
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // 3. Викликаємо ViewModel з динамічними даними
            viewModel.createNewYear(
                yearName = nextYearName,
                startDate = nextYearStartDate
            )
        }
    }

    /**
     * Ініціалізує адаптер для RecyclerView.
     * Встановлює callback [onSwitchClicked] для перемикання активного року у ViewModel,
     * та **[onLongClick] для виклику меню керування роком (Редагувати/Видалити)**.
     */
    private fun setupAdapter() { // ОБ'ЄДНАНА ФУНКЦІЯ
        yearAdapter = BeekeepingYearAdapter(
            // ВИПРАВЛЕНО: Беремо актуальний activeId з ViewModel
            activeYearId = viewModel.yearListState.value.activeYearId,

            onSwitchClicked = { year ->
                // Встановлює вибраний рік як активний у SharedPreferences
                viewModel.setActiveYear(year.yearId)
                // Закриваємо діалог після перемикання
                dismiss()
            },

            // НОВИЙ ОБРОБНИК ДОВГОГО НАТИСКАННЯ
            onLongClick = { year, anchorView ->
                showYearActionsBottomSheet(year.yearId)
            }
        )
        yearsRecyclerView.adapter = yearAdapter
    }

    /**
     * Спостерігає за StateFlow ([BeekeepingYearViewModel.yearListState]) з ViewModel.
     * Оновлює список років у RecyclerView, мітку активного року в заголовку,
     * а також зберігає ім'я активного року у [currentActiveYearName] для логіки додавання наступного року.
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.yearListState.collectLatest { state ->
                // Оновлення списку років
                yearAdapter.submitList(state.years)

                // Оновлення мітки активного року у заголовку діалогу
                val activeYear = state.years.find { it.yearId == state.activeYearId }
                if (activeYear != null) {
                    currentActiveYearName = activeYear.name

                    //val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(activeYear.startDate)

                    // ВИПРАВЛЕНО: Об'єднуємо назву року та дату в один рядок,
                    // щоб уникнути помилки "Wrong argument count"
                    val combinedInfo = activeYear.name
                    currentYearTextView.text = getString(R.string.label_current_year_status, combinedInfo)

                    yearAdapter.setActiveYear(state.activeYearId)
                } else {
                    currentYearTextView.text = getString(R.string.label_current_year_not_set)
                    currentActiveYearName = null
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // МЕТОДИ ДЛЯ НОВОГО BOTTOM SHEET ДІАЛОГУ ТА ОБРОБКИ РЕЗУЛЬТАТІВ
    // ----------------------------------------------------------------------

    /**
     * Відображає новий BottomSheetDialog з опціями "Редагувати/Видалити".
     */
    private fun showYearActionsBottomSheet(yearId: Long) {
        val fragment = YearActionsDialogFragment.newInstance(yearId)
        // Викликаємо діалог через childFragmentManager
        fragment.show(childFragmentManager, YearActionsDialogFragment.TAG)
    }

    /**
     * Налаштовує слухача для отримання результатів (вибраної дії) від YearActionsDialogFragment.
     */
    private fun setupYearActionListener() {
        // Використовуємо childFragmentManager, оскільки діалог викликається через childFragmentManager
        childFragmentManager.setFragmentResultListener(
            YearActionsDialogFragment.KEY_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            val yearId = bundle.getLong(YearActionsDialogFragment.KEY_YEAR_ID)
            val action = bundle.getString(YearActionsDialogFragment.KEY_ACTION)

            // Знаходимо об'єкт року для зручності
            val year = viewModel.yearListState.value.years.find { it.yearId == yearId }

            when (action) {
                YearActionsDialogFragment.ACTION_EDIT -> {
                    // *** НОВА ЛОГІКА: Відкриваємо діалог редагування ***
                    if (year != null) {
                        // Використовуємо parentFragmentManager, щоб EditYearDialogFragment
                        // не закрився, коли закриється BeekeeperYearDialogFragment
                        EditYearDialogFragment.newInstance(yearId).show(parentFragmentManager, EditYearDialogFragment.TAG)
                    }
                    dismiss() // Закриваємо поточний діалог
                }
                YearActionsDialogFragment.ACTION_DELETE -> {
                    // Логіка перевірки та підтвердження видалення
                    if (year != null) {
                        // Перевіряємо, чи є це єдиний рік
                        if (viewModel.yearListState.value.years.size <= 1) {
                            Toast.makeText(requireContext(), R.string.cannot_delete_last_year, Toast.LENGTH_LONG).show()
                            return@setFragmentResultListener
                        }
                        showDeleteConfirmationDialog(year)
                    }
                }
            }
        }
    }

    /**
     * Відображає діалог підтвердження перед видаленням року.
     * Залишається незмінним, тепер викликається з setupYearActionListener.
     */
    private fun showDeleteConfirmationDialog(year: BeekeepingYear) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_year_title, year.name))
            .setMessage(R.string.delete_year_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                // Викликаємо функцію видалення з ViewModel
                viewLifecycleOwner.lifecycleScope.launch {
                    val success = viewModel.deleteYear(year.yearId)
                    if (!success) {
                        // Якщо видалення не вдалося (бо це був останній рік)
                        Toast.makeText(requireContext(), R.string.cannot_delete_last_year, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.year_deleted_message, year.name), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}