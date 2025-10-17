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
 * * Клас використовує Hilt для отримання [HiveDetailViewModel] та [Fragment Result API]
 * для взаємодії з діалогами вибору кольору (ColorPickerDialogFragment).
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

        // Тепер слухаємо обидва ключі
        setupColorPickerResultListeners()
        setupDeleteConfirmationListener() // ДОДАЄМО ВИКЛИК СЛУХАЧА ВИДАЛЕННЯ
        setupEditNumberResultListener()   // ДОДАЄМО ВИКЛИК НОВОГО СЛУХАЧА

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
                dialog?.hide() // ✅ ПРИХОВУЄМО БАГАТЬКІВСЬКИЙ ДІАЛОГ!
                // ✅ ВІДКРИВАЄМО ДІАЛОГ РЕДАГУВАННЯ
                EditHiveNumberDialogFragment.newInstance(
                    hiveId,
                    currentNumber
                ).show(parentFragmentManager, EditHiveNumberDialogFragment.TAG)
            }
        }

        /** * Опція: Змінити Основний Колір. */
        view.findViewById<MaterialCardView>(R.id.selectPrimaryColorCard).setOnClickListener {
            // ВИКЛИКАЄМО НОВУ ФУНКЦІЮ ДЛЯ ОСНОВНОГО КОЛЬОРУ
            showPrimaryColorPicker(hiveId, primaryColor)
            dialog?.hide() // ✅ ПРИХОВУЄМО БАГАТЬКІВСЬКИЙ ДІАЛОГ!
        }

        /** * Опція: Змінити Додатковий Колір. */
        view.findViewById<MaterialCardView>(R.id.selectSecondaryColorCard).setOnClickListener {
            // ВИКЛИКАЄМО НОВУ ФУНКЦІЮ ДЛЯ ДОДАТКОВОГО КОЛЬОРУ
            showSecondaryColorPicker(hiveId, secondaryColor)
            dialog?.hide() // ✅ ПРИХОВУЄМО БАГАТЬКІВСЬКИЙ ДІАЛОГ!
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
            val hiveId = bundle.getLong(EditHiveNumberDialogFragment.KEY_HIVE_ID_RESULT)

            if (hiveId != 0L && !newNumber.isNullOrBlank()) {

                // ✅ ЗАЛИШАЄМО ТІЛЬКИ ОДИН ВИКЛИК: З ВАЛІДАЦІЄЮ
                viewModel.updateHiveNumberWithValidation(hiveId, newNumber)
                // ✅ ЗАКРИВАЄМО HiveOptionsDialogFragment ТУТ (після відправки даних)
                dismiss() // Закриваємо (бо успішно збережено)
            } else {
                // ❌ КОРИСТУВАЧ НАТИСНУВ "СКАСУВАТИ"
                dialog?.show() // ✅ ВІДНОВЛЮЄМО ВИДИМІСТЬ (повертаємося до опцій)
            }
        }
    }
    /**
     * Відображає діалог вибору кольору для Основного кольору.
     * Передає KEY_PRIMARY_REQUEST.
     */
    private fun showPrimaryColorPicker(hiveId: Long, initialColor: Int) {
        // ВИКОРИСТОВУЄМО КЛЮЧ PRIMARY
        val picker = ColorPickerDialogFragment.newInstance(
            initialColor,
            ColorPickerDialogFragment.KEY_PRIMARY_REQUEST
        )
        // Нам більше не потрібно додавати ARG_HIVE_ID, оскільки ми його отримуємо
        // з аргументів поточного діалогу у слухачах
        picker.show(parentFragmentManager, ColorPickerDialogFragment.TAG)
    }

    /**
     * Відображає діалог вибору кольору для Додаткового кольору.
     * Передає KEY_SECONDARY_REQUEST.
     */
    private fun showSecondaryColorPicker(hiveId: Long, initialColor: Int) {
        // ВИКОРИСТОВУЄМО КЛЮЧ SECONDARY
        val picker = ColorPickerDialogFragment.newInstance(
            initialColor,
            ColorPickerDialogFragment.KEY_SECONDARY_REQUEST
        )
        picker.show(parentFragmentManager, ColorPickerDialogFragment.TAG)
    }

    /**
     * Налаштовує ДВА окремих [Fragment Result Listener] для Основного та Додаткового кольорів.
     */
    private fun setupColorPickerResultListeners() {
        val hiveId = arguments?.getLong(ARG_HIVE_ID) ?: return // Hive ID для оновлення

        // 1. СЛУХАЧ ДЛЯ ОСНОВНОГО КОЛЬОРУ
        parentFragmentManager.setFragmentResultListener(
            ColorPickerDialogFragment.KEY_PRIMARY_REQUEST, // СЛУХАЄМО PRIMARY
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedColor = bundle.getInt(ColorPickerDialogFragment.KEY_COLOR)
            viewModel.updateHivePrimaryColor(hiveId, selectedColor) // ВИКЛИКАЄМО ТІЛЬКИ PRIMARY
            dismiss() // Закриваємо поточний діалог після обробки результату
        }

        // 2. СЛУХАЧ ДЛЯ ДОДАТКОВОГО КОЛЬОРУ
        parentFragmentManager.setFragmentResultListener(
            ColorPickerDialogFragment.KEY_SECONDARY_REQUEST, // СЛУХАЄМО SECONDARY
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedColor = bundle.getInt(ColorPickerDialogFragment.KEY_COLOR)
            viewModel.updateHiveSecondaryColor(hiveId, selectedColor) // ВИКЛИКАЄМО ТІЛЬКИ SECONDARY
            dismiss() // Закриваємо поточний діалог після обробки результату
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