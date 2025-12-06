// dialogs/BeekeeperYearDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.BeekeepingYearAdapter
import com.beemaster.beekeeperjournal.viewmodel.BeekeepingYearViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class BeekeeperYearDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: BeekeepingYearViewModel by activityViewModels()
    private lateinit var yearsRecyclerView: RecyclerView
    private lateinit var addYearButton: Button
    private lateinit var currentYearTextView: TextView
    private lateinit var yearAdapter: BeekeepingYearAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Використовуємо наданий вами layout
        return inflater.inflate(R.layout.beekeeper_year_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        yearsRecyclerView = view.findViewById(R.id.yearsRecyclerView)
        addYearButton = view.findViewById(R.id.addYearButton)
        currentYearTextView = view.findViewById(R.id.currentYearTextView)

        setupAdapter()
        observeViewModel()

        addYearButton.setOnClickListener {
            // Генеруємо ім'я нового року
            val nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1
            viewModel.createNewYear(
                yearName = "Пасічний рік $nextYear",
                startDate = Calendar.getInstance().apply { set(Calendar.YEAR, nextYear); set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            )
        }
    }

    private fun setupAdapter() {
        // Початковий ID 1L, поки не отримаємо дані зі StateFlow
        yearAdapter = BeekeepingYearAdapter(
            activeYearId = 1L,
            onSwitchClicked = { year ->
                viewModel.setActiveYear(year.yearId)
                // Можна закрити діалог, якщо потрібно
                dismiss()
            }
        )
        yearsRecyclerView.adapter = yearAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.yearListState.collectLatest { state ->
                // Оновлення списку років
                yearAdapter.submitList(state.years)

                // Оновлення мітки активного року у заголовку діалогу
                val activeYear = state.years.find { it.yearId == state.activeYearId }
                if (activeYear != null) {
                    val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(activeYear.startDate)
                    currentYearTextView.text = getString(R.string.label_current_year_status, activeYear.name, formattedDate)
                    yearAdapter.setActiveYear(state.activeYearId)
                } else {
                    currentYearTextView.text = getString(R.string.label_current_year_not_set)
                }
            }
        }
    }
}