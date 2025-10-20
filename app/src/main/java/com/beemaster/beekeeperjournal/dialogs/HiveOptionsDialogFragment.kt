package com.beemaster.beekeeperjournal.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint

/**
 * [DialogFragment] для відображення опцій конкретного вулика.
 * Клас використовує Hilt для отримання [MainActivityViewModel] та [Fragment Result API]
 * для взаємодії з діалогами вибору кольору та редагування номера.
 */
@AndroidEntryPoint
class HiveOptionsDialogFragment : DialogFragment() {

    private val viewModel: MainActivityViewModel by activityViewModels()

    override fun getTheme(): Int {
        return R.style.Theme_BeekeeperJournal_AlertDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_hive_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Налаштування слухачів Fragment Result API для всіх дочірніх діалогів
        setupColorPickerResultListeners()
        setupDeleteConfirmationListener()
        setupEditNumberResultListener()

        // Отримання ID та поточних кольорів з аргументів.
        val hiveId = arguments?.getLong(ARG_HIVE_ID) ?: 0L
        val primaryColor = arguments?.getInt(ARG_COLOR) ?: Color.BLACK
        val secondaryColor = arguments?.getInt(ARG_SECONDARY_COLOR) ?: Color.BLACK

        // Налаштування обробників натискання на картки опцій.

        /** * Опція: Редагувати Номер Вулика. */
        view.findViewById<MaterialCardView>(R.id.editNumberCard).setOnClickListener {
            val hiveId = arguments?.getLong(ARG_HIVE_ID) ?: 0L
            val currentNumber = arguments?.getString(ARG_HIVE_NUMBER) ?: ""

            if (hiveId != 0L) {
                dialog?.hide() // ✅ ПРИХОВУЄМО БАТЬКІВСЬКИЙ ДІАЛОГ
                // ВІДКРИВАЄМО ДІАЛОГ РЕДАГУВАННЯ
                EditHiveNumberDialogFragment.newInstance(
                    hiveId,
                    currentNumber
                ).show(parentFragmentManager, EditHiveNumberDialogFragment.TAG)
            }
        }

        /** * Опція: Змінити Основний Колір. */
        view.findViewById<MaterialCardView>(R.id.selectPrimaryColorCard).setOnClickListener {
            dialog?.hide() // ✅ ПРИХОВУЄМО БАТЬКІВСЬКИЙ ДІАЛОГ
            showPrimaryColorPicker(hiveId, primaryColor)
        }

        /** * Опція: Змінити Додатковий Колір. */
        view.findViewById<MaterialCardView>(R.id.selectSecondaryColorCard).setOnClickListener {
            dialog?.hide() // ✅ ПРИХОВУЄМО БАТЬКІВСЬКИЙ ДІАЛОГ
            showSecondaryColorPicker(hiveId, secondaryColor)
        }

        /** * Опція: Видалити Вулик. */
        view.findViewById<MaterialCardView>(R.id.deleteHiveCard).setOnClickListener {
            val hiveId = arguments?.getLong(ARG_HIVE_ID) ?: 0L

            if (hiveId != 0L) {
                // ВІДКРИВАЄМО ДІАЛОГ ПІДТВЕРДЖЕННЯ
                ConfirmDeleteDialogFragment.newInstance(
                    hiveId,
                    "вулик"
                ).show(parentFragmentManager, ConfirmDeleteDialogFragment.TAG)
            }
        }
    }

    /**
     * Налаштовує слухача для редагування номера вулика.
     */
    private fun setupEditNumberResultListener() {
        parentFragmentManager.setFragmentResultListener(
            EditHiveNumberDialogFragment.KEY_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->

            val newNumber = bundle.getString(EditHiveNumberDialogFragment.KEY_NEW_NUMBER)
            // Використовуємо уніфікований ключ
            val hiveId = bundle.getLong(EditHiveNumberDialogFragment.KEY_HIVE_ID)

            if (hiveId != 0L && !newNumber.isNullOrBlank()) {
                // УСПІХ/ЗБЕРЕЖЕННЯ
                viewModel.updateHiveNumberWithValidation(hiveId, newNumber)
            }

            // ✅ ЗАКРИВАЄМО В БУДЬ-ЯКОМУ ВИПАДКУ (УСПІХ, БЕЗ ЗМІН, АБО СКАСУВАННЯ)
            // Це видаляє діалог з FragmentManager і запобігає "оживанню".
            dismiss()
        }
    }

    /**
     * Відображає діалог вибору кольору для Основного кольору.
     */
    private fun showPrimaryColorPicker(hiveId: Long, initialColor: Int) {
        val picker = ColorPickerDialogFragment.newInstance(
            initialColor,
            ColorPickerDialogFragment.KEY_PRIMARY_REQUEST
        )
        picker.show(parentFragmentManager, ColorPickerDialogFragment.TAG)
    }

    /**
     * Відображає діалог вибору кольору для Додаткового кольору.
     */
    private fun showSecondaryColorPicker(hiveId: Long, initialColor: Int) {
        val picker = ColorPickerDialogFragment.newInstance(
            initialColor,
            ColorPickerDialogFragment.KEY_SECONDARY_REQUEST
        )
        picker.show(parentFragmentManager, ColorPickerDialogFragment.TAG)
    }

    /**
     * Налаштовує ДВА окремих [Fragment Result Listener] для Основного та Додаткового кольорів.
     * Обробляє як успішний вибір, так і скасування (через KEY_CANCELED).
     */
    private fun setupColorPickerResultListeners() {
        val hiveId = arguments?.getLong(ARG_HIVE_ID) ?: return

        // 1. СЛУХАЧ ДЛЯ ОСНОВНОГО КОЛЬОРУ
        parentFragmentManager.setFragmentResultListener(
            ColorPickerDialogFragment.KEY_PRIMARY_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedColor = bundle.getInt(ColorPickerDialogFragment.KEY_COLOR)
            // ✅ Отримуємо прапорець скасування
            val isCanceled = bundle.getBoolean(ColorPickerDialogFragment.KEY_CANCELED, false)

            if (!isCanceled) {
                viewModel.updateHivePrimaryColor(hiveId, selectedColor)
            }

            // ✅ БЕЗУМОВНО ЗАКРИВАЄМО ПІСЛЯ ОТРИМАННЯ РЕЗУЛЬТАТУ
            dismiss()
        }

        // 2. СЛУХАЧ ДЛЯ ДОДАТКОВОГО КОЛЬОРУ
        parentFragmentManager.setFragmentResultListener(
            ColorPickerDialogFragment.KEY_SECONDARY_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedColor = bundle.getInt(ColorPickerDialogFragment.KEY_COLOR)
            // ✅ Отримуємо прапорець скасування
            val isCanceled = bundle.getBoolean(ColorPickerDialogFragment.KEY_CANCELED, false)

            if (!isCanceled) {
                viewModel.updateHiveSecondaryColor(hiveId, selectedColor)
            }

            // ✅ БЕЗУМОВНО ЗАКРИВАЄМО ПІСЛЯ ОТРИМАННЯ РЕЗУЛЬТАТУ
            dismiss()
        }
    }

    /**
     * Налаштовує слухача для результату діалогу підтвердження видалення.
     */
    private fun setupDeleteConfirmationListener() {
        parentFragmentManager.setFragmentResultListener(
            ConfirmDeleteDialogFragment.KEY_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->

            val confirmed = bundle.getBoolean(ConfirmDeleteDialogFragment.KEY_CONFIRMED, false)
            val hiveIdToDelete = arguments?.getLong(ARG_HIVE_ID) ?: 0L

            if (confirmed && hiveIdToDelete != 0L) {
                viewModel.deleteHive(hiveIdToDelete)
            }
            dismiss()
        }
    }

    // ✅ МЕТОД setupColorPickerCancelListener ТА onActivityResult БІЛЬШЕ НЕ ПОТРІБНІ І ВИДАЛЕНІ

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    companion object {
        const val TAG = "HiveOptionsDialog"

        // Ключі для аргументів Fragment.
        private const val ARG_HIVE_ID = "hive_id"
        private const val ARG_HIVE_NUMBER = "hive_number"
        private const val ARG_COLOR = "color"
        private const val ARG_SECONDARY_COLOR = "secondary_color"

        fun newInstance(
            id: Long,
            hiveNumber: String,
            color: Int,
            secondaryColor: Int
        ) = HiveOptionsDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_HIVE_ID, id)
                putString(ARG_HIVE_NUMBER, hiveNumber)
                putInt(ARG_COLOR, color)
                putInt(ARG_SECONDARY_COLOR, secondaryColor)
            }
        }
    }
}