package com.beemaster.beekeeperjournal.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.utils.BackupPrefsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Bottom Sheet для вибору опцій синхронізації.
 * Тепер включає Ручний Експорт/Імпорт.
 *
 * ✅ Оновлено: Видалено логіку вибору каталогу для автобекапу.
 */
@AndroidEntryPoint
class SyncOptionsDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SyncOptionsDialogFragment"
    }

    // Інтерфейс для передачі дій назад до Activity
    interface SyncOptionsListener {
        fun onExportSelected()
        fun onImportSelected()
        // ❌ ВИДАЛЕНО: fun onSelectBackupFolder()
    }

    private lateinit var listener: SyncOptionsListener

    @Inject
    lateinit var prefsManager: BackupPrefsManager // Залишено, але не використовується для URI

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onAttach(context: Context) {
        super.onAttach(context)
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

        val cardExport: MaterialCardView = view.findViewById(R.id.card_create_backup)
        val cardImport: MaterialCardView = view.findViewById(R.id.card_restore_backup)
        val pathTextView: TextView = view.findViewById(R.id.backup_path_summary)

        // Слухачі
        cardExport.setOnClickListener {
            listener.onExportSelected()
            dismiss()
        }

        cardImport.setOnClickListener {
            listener.onImportSelected()
            dismiss()
        }

        updatePathDisplay(pathTextView)

        return view
    }

    /**
     * Оновлює TextView, щоб відобразити поточний статус бекапу у внутрішній пам'яті.
     */
    private fun updatePathDisplay(pathTextView: TextView) {
        // Оскільки автобекап тепер працює у внутрішній пам'яті програми,
        // ми завжди відображаємо фіксоване повідомлення про його місцезнаходження.
        // Припускаємо, що R.string.auto_backup_location_internal було додано до strings.xml
        pathTextView.text = getString(R.string.auto_backup_location_internal)
    }
}