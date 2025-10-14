// HiveOptionsDialogFragment.kt

package com.beemaster.beekeeperjournal.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.beemaster.beekeeperjournal.R
import com.google.android.material.card.MaterialCardView

// Цей клас відповідає за відображення діалогового вікна опцій вулика
class HiveOptionsDialogFragment : DialogFragment() {

    // Інтерфейс для передачі результатів натискання назад до Activity/Fragment
    interface HiveOptionListener {
        fun onEditNumberClicked()
        fun onSelectPrimaryColorClicked()
        fun onSelectSecondaryColorClicked()
        fun onDeleteHiveClicked()
    }

    // Зберігаємо посилання на слухача (Activity/Fragment)
    private var listener: HiveOptionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Використовуємо наш чистий XML-файл для розмітки діалогу
        return inflater.inflate(R.layout.dialog_hive_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Встановлюємо слухачів натискання на картки
        view.findViewById<MaterialCardView>(R.id.editNumberCard).setOnClickListener {
            listener?.onEditNumberClicked()
            dismiss() // Закриваємо діалог після натискання
        }

        view.findViewById<MaterialCardView>(R.id.selectPrimaryColorCard).setOnClickListener {
            listener?.onSelectPrimaryColorClicked()
            dismiss()
        }

        view.findViewById<MaterialCardView>(R.id.selectSecondaryColorCard).setOnClickListener {
            listener?.onSelectSecondaryColorClicked()
            dismiss()
        }

        view.findViewById<MaterialCardView>(R.id.deleteHiveCard).setOnClickListener {
            listener?.onDeleteHiveClicked()
            dismiss()
        }
    }

    // Метод для встановлення слухача з Activity/Fragment
    fun setHiveOptionListener(listener: HiveOptionListener) {
        this.listener = listener
    }

    // Забезпечуємо, що вікно має повну ширину (опціонально, для гарного вигляду)
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    companion object {
        const val TAG = "HiveOptionsDialog"

        // Додайте константи для ключів аргументів
        private const val ARG_HIVE_ID = "hive_id"
        private const val ARG_HIVE_NUMBER = "hive_number"
        private const val ARG_COLOR = "color"
        private const val ARG_SECONDARY_COLOR = "secondary_color"

        // ✅ ВИПРАВЛЕНА ФУНКЦІЯ newInstance
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