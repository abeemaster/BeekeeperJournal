// Файл: NoteActionsDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.beemaster.beekeeperjournal.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * BottomSheetDialogFragment для вибору дій над нотаткою (Редагувати/Видалити).
 * Повертає обрану дію та ID нотатки через Fragment Result API.
 */
class NoteActionsDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "NoteActionsDialog"

        // Ключі для Fragment Result API
        const val KEY_REQUEST = "noteActionsRequest"
        const val KEY_NOTE_ID = "noteId"
        const val KEY_ACTION = "action"

        // Значення дій
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"

        private const val ARG_NOTE_ID = "noteIdArg"

        fun newInstance(noteId: Int): NoteActionsDialogFragment {
            return NoteActionsDialogFragment().apply {
                arguments = bundleOf(
                    ARG_NOTE_ID to noteId
                )
            }
        }
    }

    private val noteId: Int
        get() = arguments?.getInt(ARG_NOTE_ID) ?: 0

    // Використовуємо тему для заокруглених кутів
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_note_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editCard = view.findViewById<MaterialCardView>(R.id.editNoteCard)
        val deleteCard = view.findViewById<MaterialCardView>(R.id.deleteNoteCard)

        // Обробка натискання "Редагувати"
        editCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_NOTE_ID to noteId,
                KEY_ACTION to ACTION_EDIT // Надсилаємо дію "edit"
            ))
            dismiss()
        }

        // Обробка натискання "Видалити"
        deleteCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_NOTE_ID to noteId,
                KEY_ACTION to ACTION_DELETE // Надсилаємо дію "delete"
            ))
            dismiss()
        }
    }
}