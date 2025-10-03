// SectionsPagerAdapter.kt
// Цей адаптер керує відображенням фрагментів у ViewPager.

package com.beemaster.beekeeperjournal.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.beemaster.beekeeperjournal.fragments.ExpensesFragment
import com.beemaster.beekeeperjournal.fragments.IncomesFragment

class SectionsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val TAB_COUNT = 2
    }

    override fun getItemCount(): Int {
        return TAB_COUNT // Використовуємо константу
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ExpensesFragment()
            1 -> IncomesFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}