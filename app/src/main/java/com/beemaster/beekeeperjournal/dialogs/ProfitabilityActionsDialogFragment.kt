// Файл: ProfitabilityActionsDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.beemaster.beekeeperjournal.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

/**
 * BottomSheetDialogFragment для вибору дій над записом рентабельності (Витрати/Прибутки).
 * Опції: Редагувати / Видалити.
 */
class ProfitabilityActionsDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ProfitActionsDialog"

        // Ключі для Fragment Result API
        const val KEY_REQUEST = "profitActionsRequest"
        const val KEY_ENTRY_ID = "entryId"
        const val KEY_ACTION = "action"
        const val KEY_ENTRY_TYPE = "entryType" // Щоб знати, чи це Витрати чи Прибутки

        // Значення дій
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"

        private const val ARG_ENTRY_ID = "entryIdArg"
        private const val ARG_ENTRY_TYPE = "entryTypeArg"


        fun newInstance(entryId: Long, entryType: String): ProfitabilityActionsDialogFragment {
            return ProfitabilityActionsDialogFragment().apply {
                arguments = bundleOf(
                    ARG_ENTRY_ID to entryId,
                    ARG_ENTRY_TYPE to entryType
                )
            }
        }
    }

    private val entryId: Long
        get() = arguments?.getLong(ARG_ENTRY_ID) ?: 0L

    private val entryType: String
        get() = arguments?.getString(ARG_ENTRY_TYPE) ?: ""

    // Забезпечуємо єдинообразний стиль контейнера (заокруглення)
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Використовуємо наш уніфікований макет для Редагувати/Видалити
        return inflater.inflate(R.layout.dialog_note_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ID елементів взяті з уніфікованого макета dialog_note_actions.xml
        val editCard = view.findViewById<MaterialCardView>(R.id.editNoteCard)
        val deleteCard = view.findViewById<MaterialCardView>(R.id.deleteNoteCard)

        // Обробка натискання "Редагувати"
        editCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_ENTRY_ID to entryId,
                KEY_ENTRY_TYPE to entryType,
                KEY_ACTION to ACTION_EDIT
            ))
            dismiss()
        }

        // Обробка натискання "Видалити"
        deleteCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_ENTRY_ID to entryId,
                KEY_ENTRY_TYPE to entryType,
                KEY_ACTION to ACTION_DELETE
            ))
            dismiss()
        }
    }
}