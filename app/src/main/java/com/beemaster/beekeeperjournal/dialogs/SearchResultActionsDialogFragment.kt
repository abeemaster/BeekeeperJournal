// Файл: SearchResultActionsDialogFragment.kt

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
 * BottomSheetDialogFragment для вибору дій над нотаткою з результатів пошуку.
 * Опції: Перейти до вулика / Редагувати запис.
 * Повертає обрану дію, ID нотатки та ID вулика через Fragment Result API.
 */
class SearchResultActionsDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SearchResultActionsDialog"

        // Ключі для Fragment Result API
        const val KEY_REQUEST = "searchActionsRequest"
        const val KEY_NOTE_ID = "noteId"
        const val KEY_HIVE_ID = "hiveId" // Важливо: додаємо ID вулика
        const val KEY_ACTION = "action"

        // Значення дій
        const val ACTION_GO_TO_HIVE = "goToHive"
        const val ACTION_EDIT_RECORD = "editRecord"

        private const val ARG_NOTE_ID = "noteIdArg"
        private const val ARG_HIVE_ID = "hiveIdArg"

        fun newInstance(noteId: Int, hiveId: Int): SearchResultActionsDialogFragment {
            return SearchResultActionsDialogFragment().apply {
                arguments = bundleOf(
                    ARG_NOTE_ID to noteId,
                    ARG_HIVE_ID to hiveId
                )
            }
        }
    }

    private val noteId: Int
        get() = arguments?.getInt(ARG_NOTE_ID) ?: 0

    private val hiveId: Int
        get() = arguments?.getInt(ARG_HIVE_ID) ?: 0

    // Забезпечуємо єдинообразний стиль контейнера (заокруглення)
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Використовуємо наш новий уніфікований макет
        return inflater.inflate(R.layout.dialog_search_result_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goToHiveCard = view.findViewById<MaterialCardView>(R.id.goToHiveCard)
        val editRecordCard = view.findViewById<MaterialCardView>(R.id.editRecordCard)

        // Обробка натискання "Перейти у вулик"
        goToHiveCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_NOTE_ID to noteId,
                KEY_HIVE_ID to hiveId,
                KEY_ACTION to ACTION_GO_TO_HIVE
            ))
            dismiss()
        }

        // Обробка натискання "Редагувати запис"
        editRecordCard.setOnClickListener {
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_NOTE_ID to noteId,
                KEY_HIVE_ID to hiveId,
                KEY_ACTION to ACTION_EDIT_RECORD
            ))
            dismiss()
        }
    }
}