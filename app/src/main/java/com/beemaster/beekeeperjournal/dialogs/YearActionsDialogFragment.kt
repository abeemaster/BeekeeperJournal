// dialogs/YearActionsDialogFragment.kt

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
 * BottomSheetDialogFragment для вибору дій над пасічним роком (Редагувати/Видалити).
 * Використовує той же макет, що і NoteActionsDialogFragment (R.layout.dialog_note_actions).
 * Повертає обрану дію та ID року через Fragment Result API.
 */
// ЗВЕРНІТЬ УВАГУ: Клас називається YearActionsDialogFragment, але успадковується від BottomSheetDialogFragment
class YearActionsDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "YearActionsDialog"

        // Ключі для Fragment Result API
        const val KEY_REQUEST = "yearActionsRequest" // Унікальний ключ для року
        const val KEY_YEAR_ID = "yearId" // Використовуємо Long
        const val KEY_ACTION = "action"

        // Значення дій
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"

        private const val ARG_YEAR_ID = "yearIdArg"

        fun newInstance(yearId: Long): YearActionsDialogFragment {
            return YearActionsDialogFragment().apply {
                arguments = bundleOf(
                    ARG_YEAR_ID to yearId
                )
            }
        }
    }

    private val yearId: Long // Змінили тип на Long
        get() = arguments?.getLong(ARG_YEAR_ID) ?: 0L

    // Використовуємо тему для заокруглених кутів
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Використовуємо ІСНУЮЧИЙ макет для нотаток
        return inflater.inflate(R.layout.dialog_note_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ідентифікатори з макету R.layout.dialog_note_actions
        val editCard = view.findViewById<MaterialCardView>(R.id.editNoteCard)
        val deleteCard = view.findViewById<MaterialCardView>(R.id.deleteNoteCard)

        // Обробка натискання "Редагувати"
        editCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_YEAR_ID to yearId,
                KEY_ACTION to ACTION_EDIT // Надсилаємо дію "edit"
            ))
            dismiss()
        }

        // Обробка натискання "Видалити"
        deleteCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_YEAR_ID to yearId,
                KEY_ACTION to ACTION_DELETE // Надсилаємо дію "delete"
            ))
            dismiss()
        }
    }
}