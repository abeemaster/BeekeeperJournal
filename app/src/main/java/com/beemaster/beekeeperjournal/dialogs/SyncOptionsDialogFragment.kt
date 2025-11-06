package com.beemaster.beekeeperjournal.dialogs

import android.content.Context
import android.net.Uri
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
 * Тепер включає Ручний Експорт/Імпорт та Вибір Каталогу для Автобекапу.
 */
@AndroidEntryPoint
class SyncOptionsDialogFragment : BottomSheetDialogFragment() {

    // ДОДАНО: Companion Object та TAG для виклику з SettingsActivity
    companion object {
        const val TAG = "SyncOptionsDialogFragment"
    }

    // Інтерфейс для передачі дій назад до Activity
    interface SyncOptionsListener {
        fun onExportSelected()
        fun onImportSelected()
        fun onSelectBackupFolder()
    }

    private lateinit var listener: SyncOptionsListener

    @Inject
    lateinit var prefsManager: BackupPrefsManager

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
        val cardSelectFolder: MaterialCardView = view.findViewById(R.id.card_select_backup_folder)
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

        cardSelectFolder.setOnClickListener {
            listener.onSelectBackupFolder()
            dismiss()
        }

        updatePathDisplay(pathTextView)

        return view
    }

    /**
     * Оновлює TextView, щоб відобразити поточний вибраний каталог для автобекапу.
     */
    private fun updatePathDisplay(pathTextView: TextView) {
        // ✅ ВИПРАВЛЕННЯ: Прибираємо аргумент 'uri'
        val uri: Uri? = prefsManager.getBackupDirectoryUri()

        if (uri != null) {
            // Отримуємо ідентифікатор документа, який зазвичай є ім'ям каталогу в SAF
            // Ми використовуємо URI.path, оскільки DocumentsContract може вимагати особливих дозволів
            val pathString = uri.path ?: uri.toString()

            // Відображаємо частину шляху
            val displayPath = if (pathString.length > 30) {
                "...${pathString.substring(pathString.length - 30)}"
            } else {
                pathString
            }

            pathTextView.text = getString(R.string.current_backup_path, displayPath)
        } else {
            pathTextView.text = getString(R.string.backup_directory_not_set)
        }
    }
}