// ProfitabilityActivity.kt
// Цей файл відповідає за відображення екрана рентабельності.

package com.beemaster.beekeeperjournal.activities

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.beemaster.beekeeperjournal.R
import androidx.lifecycle.lifecycleScope
import com.beemaster.beekeeperjournal.adapters.SectionsPagerAdapter
import com.beemaster.beekeeperjournal.databinding.ActivityProfitabilityBinding
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Activity для відображення екрана річної рентабельності.
 * Включає ViewPager2 з двома вкладками: Витрати та Прибутки.
 */
@AndroidEntryPoint
class ProfitabilityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfitabilityBinding
    private val viewModel: ProfitabilityViewModel by viewModels()

    /**
     * Викликається при першому створенні Activity.
     * Ініціалізує View Binding, Toolbar, налаштовує ViewPager2 та TabLayout,
     * а також починає спостереження за даними рентабельності.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfitabilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onSupportNavigateUp() }

        val sectionsPagerAdapter = SectionsPagerAdapter(this)
        binding.viewPager.adapter = sectionsPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_title_expenses) // ✅ Локалізовано
                1 -> getString(R.string.tab_title_income)   // ✅ Локалізовано
                else -> throw IllegalArgumentException(getString(R.string.error_invalid_tab_position)) // ✅ Локалізовано
            }
        }.attach()

        observeProfitability()
    }

    /**
     * Спостерігає за значенням річної рентабельності у ViewModel (Flow<Double>) та оновлює UI.
     * Встановлює колір тексту відповідно до знаку суми (позитивний, негативний, нуль).
     */
    private fun observeProfitability() {
        lifecycleScope.launch {
            viewModel.profitability.collect { profitability ->
                val amountText: String
                val colorId: Int

                if (profitability > 0) {
                    amountText = String.format(Locale.getDefault(), "+%.2f", profitability)
                    colorId = R.color.color_primary
                } else if (profitability < 0) {
                    amountText = String.format(Locale.getDefault(), "%.2f", profitability)
                    colorId = R.color.status_red
                } else {
                    amountText = String.format(Locale.getDefault(), "%.2f", profitability)
                    colorId = R.color.status_blue
                }

                // ✅ Використовуємо форматний рядок для локалізації
                val fullText = getString(R.string.annual_profitability_format, amountText)
                val spannableString = SpannableString(fullText)

                val startIndex = fullText.indexOf(amountText)
                if (startIndex != -1) {
                    val endIndex = startIndex + amountText.length
                    // Встановлюємо колір для суми
                    spannableString.setSpan(
                        ForegroundColorSpan(
                            ContextCompat.getColor(
                                this@ProfitabilityActivity,
                                colorId
                            )
                        ),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                binding.yearAmount.text = spannableString
            }
        }
    }

    /**
     * Обробляє натискання на кнопку "Вгору" (стрілка назад) на панелі інструментів.
     * @return true, якщо перехід виконано.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}