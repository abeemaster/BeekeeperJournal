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
 * Включає Ручний Експорт/Імпорт та Налаштування Каталогу для Автобекапу.
 *
 * Відновлено логіку вибору каталогу, необхідну для копіювання автобекапу в SAF.
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
        fun onSelectBackupFolder()
    }

    private lateinit var listener: SyncOptionsListener

    @Inject
    // Тепер використовуємо prefsManager для отримання URI SAF
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
        // Припускаємо, що ви повернули елемент card_select_backup_folder у ваш XML
        val view = inflater.inflate(R.layout.dialog_sync_options, container, false)

        val cardExport: MaterialCardView = view.findViewById(R.id.card_create_backup)
        val cardImport: MaterialCardView = view.findViewById(R.id.card_restore_backup)

        // Елемент для вибору папки
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

        // Слухач для вибору папки
        cardSelectFolder.setOnClickListener {
            listener.onSelectBackupFolder()
            dismiss()
        }

        updatePathDisplay(pathTextView)

        return view
    }

    /**
     * Оновлює TextView, щоб відобразити поточний вибраний каталог для SAF копіювання.
     */
    private fun updatePathDisplay(pathTextView: TextView) {
        val uri: Uri? = prefsManager.getBackupDirectoryUri()

        if (uri != null) {
            // Ми не можемо отримати людське ім'я папки тут без DocumentsContract (який ми уникаємо),
            // тому відображаємо загальний статус та останню частину Uri для підтвердження.
            val pathString = uri.path ?: uri.toString()

            val displayPath = if (pathString.length > 30) {
                "...${pathString.substring(pathString.length - 30)}"
            } else {
                pathString
            }

            // Припускаємо, що R.string.current_saf_path існує у strings.xml
            pathTextView.text = getString(R.string.current_saf_path, displayPath)
        } else {
            // Припускаємо, що R.string.backup_directory_not_set існує у strings.xml
            pathTextView.text = getString(R.string.backup_directory_not_set)
        }
    }
}