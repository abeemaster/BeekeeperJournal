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

@AndroidEntryPoint
class ProfitabilityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfitabilityBinding
    private val viewModel: ProfitabilityViewModel by viewModels()

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
                0 -> "Витрати"
                1 -> "Прибутки"
                else -> throw IllegalArgumentException("Недійсна позиція")
            }
        }.attach()

        observeProfitability()
    }

    /**
     * Спостерігає за рентабельністю у ViewModel та оновлює UI.
     */
    private fun observeProfitability() {
        lifecycleScope.launch {
            viewModel.profitability.collect { profitability ->
                val amountText: String
                val colorId: Int

                if (profitability > 0) {
                    amountText = String.format(Locale.getDefault(), "+%.2f", profitability)
                    colorId = R.color.profit_positive
                } else if (profitability < 0) {
                    amountText = String.format(Locale.getDefault(), "%.2f", profitability)
                    colorId = R.color.profit_negative
                } else {
                    amountText = String.format(Locale.getDefault(), "%.2f", profitability)
                    colorId = R.color.profit_zero
                }

                val fullText = "Річна рентабельність:  $amountText"
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
