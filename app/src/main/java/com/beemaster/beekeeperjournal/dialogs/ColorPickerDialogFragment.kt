// ColorPickerDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.beemaster.beekeeperjournal.R
import dagger.hilt.android.AndroidEntryPoint


/**
 * Діалогове вікно для вибору кольору вулика зі спливаючої сітки.
 * Вибір кольору негайно закриває діалог і повертає результат через Fragment Result API.
 */
@AndroidEntryPoint
class ColorPickerDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "ColorPickerDialog"
        const val KEY_COLOR = "selectedColor"
        // Використовуватимемо ці ключі для Fragment Result API
        const val KEY_PRIMARY_REQUEST = "primary_color_request"
        const val KEY_SECONDARY_REQUEST = "secondary_color_request"

        // НОВИЙ АРГУМЕНТ ДЛЯ ЗБЕРЕЖЕННЯ КЛЮЧА, ЯКИЙ ПОТРІБНО ПОВЕРНУТИ
        private const val ARG_REQUEST_KEY = "requestKey"
        private const val ARG_INITIAL_COLOR = "initialColor"

        private const val COLOR_CIRCLE_SIZE_DP = 48
        private const val COLOR_CIRCLE_MARGIN_DP = 8
        private const val SELECTED_OUTLINE_WIDTH_DP = 4
        private const val DEFAULT_OUTLINE_WIDTH_DP = 2

        private val HIVE_COLORS = intArrayOf(
            Color.WHITE,        // Білий
            0xFFFFEB3B.toInt(), // Жовтий
            0xFF2199F3.toInt(), // Синій
            0xFF4CAF50.toInt(), // Зелений
            0xFFF44336.toInt(), // Червоний
            0xFFFF9800.toInt(), // Помаранчевий
            0xFF9C27B0.toInt(), // Фіолетовий
            0xFF607D8B.toInt(), // Сірий

        )

        // ОНОВЛЕНИЙ newInstance: ТЕПЕР ВІН ПРИЙМАЄ requestKey
        fun newInstance(@ColorInt initialColor: Int, requestKey: String): ColorPickerDialogFragment {
            return ColorPickerDialogFragment().apply {
                arguments = bundleOf(
                    ARG_INITIAL_COLOR to initialColor,
                    ARG_REQUEST_KEY to requestKey // ЗБЕРІГАЄМО КЛЮЧ
                )
            }
        }
    }

    private lateinit var colorGrid: GridLayout
    @ColorInt
    private var initialColor: Int = Color.BLACK
    // ЗМІННА ДЛЯ ЗБЕРЕЖЕННЯ КЛЮЧА ЗАПИТУ
    private var requestKey: String = KEY_PRIMARY_REQUEST

    private var selectedView: View? = null

    override fun getTheme(): Int {
        return R.style.Theme_BeekeeperJournal_AlertDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initialColor = arguments?.getInt(ARG_INITIAL_COLOR) ?: Color.BLACK
        // ОТРИМУЄМО КЛЮЧ, ЯКИЙ ПОТРІБНО ПОВЕРНУТИ
        requestKey = arguments?.getString(ARG_REQUEST_KEY) ?: KEY_PRIMARY_REQUEST

        return inflater.inflate(R.layout.dialog_color_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        colorGrid = view.findViewById(R.id.colorGrid)

        val density = resources.displayMetrics.density
        val sizePx = (COLOR_CIRCLE_SIZE_DP * density).toInt()
        val marginPx = (COLOR_CIRCLE_MARGIN_DP * density).toInt()
        val selectedOutlinePx = (SELECTED_OUTLINE_WIDTH_DP * density).toInt()
        val defaultOutlinePx = (DEFAULT_OUTLINE_WIDTH_DP * density).toInt()

        HIVE_COLORS.forEach { color ->
            val colorCircle = createColorCircle(color, sizePx, marginPx, defaultOutlinePx)

            colorCircle.setOnClickListener {
                handleColorSelection(it, color)
            }
            colorGrid.addView(colorCircle)

            if (color == initialColor) {
                setSelectedOutline(colorCircle, selectedOutlinePx)
                selectedView = colorCircle
            }
        }
    }

    /**
     * Створює View-круг з кольоровим фоном та постійним чорним обідком.
     */
    private fun createColorCircle(@ColorInt color: Int, sizePx: Int, marginPx: Int, defaultOutlinePx: Int): View {
        val circle = View(requireContext())

        val layoutParams = GridLayout.LayoutParams().apply {
            width = sizePx
            height = sizePx
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }
        circle.layoutParams = layoutParams

        val colorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(defaultOutlinePx, Color.BLACK)
        }
        circle.background = colorDrawable

        circle.isClickable = true
        circle.isFocusable = true

        // ВИПРАВЛЕННЯ 1: Використовуємо стандартний drawable для ripple-ефекту з теми
        val outValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        val rippleDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), outValue.resourceId)
        circle.foreground = rippleDrawable

        circle.tag = color

        return circle
    }

    /**
     * Обробляє вибір кольору: оновлює виділення, повертає результат і закриває діалог.
     */
    private fun handleColorSelection(view: View, @ColorInt color: Int) {
        val density = resources.displayMetrics.density
        val selectedOutlinePx = (SELECTED_OUTLINE_WIDTH_DP * density).toInt()
        val defaultOutlinePx = (DEFAULT_OUTLINE_WIDTH_DP * density).toInt()

        (selectedView?.background as? GradientDrawable)?.setStroke(defaultOutlinePx, Color.BLACK)

        setSelectedOutline(view, selectedOutlinePx)
        selectedView = view

        // ВИКОРИСТОВУЄМО ЗБЕРЕЖЕНИЙ requestKey
        setFragmentResult(requestKey, bundleOf(KEY_COLOR to color))
        dismiss()
    }

    /**
     * Встановлює кольорову рамку навколо вибраного круга.
     */
    private fun setSelectedOutline(view: View, outlinePx: Int) {
        val typedValue = android.util.TypedValue()
        val resolved = requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimaryVariant,
            typedValue,
            true
        )

        if (resolved) {
            @androidx.annotation.ColorInt
            val colorPrimary: Int

            if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {

                colorPrimary = typedValue.data
            } else {
                colorPrimary = androidx.core.content.ContextCompat.getColor(requireContext(), typedValue.resourceId)
            }

            // Застосовуємо колір
            (view.background as? android.graphics.drawable.GradientDrawable)?.setStroke(outlinePx, colorPrimary)
        } else {
            // Запасний варіант
            (view.background as? android.graphics.drawable.GradientDrawable)?.setStroke(outlinePx, android.graphics.Color.RED)
        }
    }
}