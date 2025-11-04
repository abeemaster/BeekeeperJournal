package com.beemaster.beekeeperjournal.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.beemaster.beekeeperjournal.R
import com.google.android.material.card.MaterialCardView

/**
 * Bottom Sheet для вибору опцій синхронізації (Створити/Відновити резервну копію).
 *
 * Примітка: Ми використовуємо BottomSheetDialogFragment, щоб діалог з'являвся знизу
 * та автоматично застосовував заокруглення кутів.
 */
class SyncOptionsDialogFragment : BottomSheetDialogFragment() {

    // Інтерфейс для передачі дій назад до Activity/Fragment
    interface SyncOptionsListener {
        fun onExportSelected()
        fun onImportSelected()
    }

    private lateinit var listener: SyncOptionsListener

    // Використовуємо ваш кастомний стиль для Bottom Sheet
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Перевіряємо, чи реалізує батьківський контекст наш інтерфейс
        if (context is SyncOptionsListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement SyncOptionsListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_sync_options, container, false)

        // ✅ Знаходимо карточки
        val cardExport: MaterialCardView = view.findViewById(R.id.card_create_backup)
        val cardImport: MaterialCardView = view.findViewById(R.id.card_restore_backup)

        // Встановлюємо слухача на карточку для обробки натискання
        cardExport.setOnClickListener {
            listener.onExportSelected()
            dismiss()
        }

        cardImport.setOnClickListener {
            listener.onImportSelected()
            dismiss()
        }

        return view
    }
}
