// ProfitabilityActivity.kt
// Цей файл відповідає за відображення екрана рентабельності.

package com.beemaster.beekeeperjournal.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.SectionsPagerAdapter
import com.beemaster.beekeeperjournal.databinding.ActivityProfitabilityBinding
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

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
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val sectionsPagerAdapter = SectionsPagerAdapter(this)
        binding.viewPager.adapter = sectionsPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Витрати"
                1 -> "Прибутки"
                else -> throw IllegalArgumentException("Invalid position")
            }
        }.attach()
    }
}
