// DrawerManager файл що відповідає за роботу бічної панелі

package com.beemaster.beekeeperjournal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import android.widget.Toast
import android.view.MenuItem
import android.view.Menu
import android.view.View

object DrawerManager {

    fun setupDrawer(activity: AppCompatActivity, dataSynchronizer: DataSynchronizer? = null) {
        val drawerLayout: DrawerLayout = activity.findViewById(R.id.drawer_layout)
        val navView: NavigationView = activity.findViewById(R.id.nav_view)
        val drawerToggleButton: ImageButton = activity.findViewById(R.id.drawer_toggle_button)

        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        activity.onBackPressedDispatcher.addCallback(activity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    this.isEnabled = false
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Додаємо слухача для закриття панелі
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                deselectAllMenuItems(navView)
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (activity is MainActivity) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        val intent = Intent(activity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        activity.startActivity(intent)
                    }
                }
                R.id.nav_general_notes -> {
                    if (activity is HiveInfoActivity && activity.intent.getStringExtra("TYPE") == "general") {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        val intent = Intent(activity, HiveInfoActivity::class.java).apply {
                            putExtra("TYPE", "general")
                            putExtra("TITLE", "Загальні записи")
                            putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, "Загальні записи")
                        }
                        activity.startActivity(intent)
                    }
                }
                R.id.nav_search -> {
                    if (activity is SearchActivity) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        val intent = Intent(activity, SearchActivity::class.java)
                        activity.startActivity(intent)
                    }
                }
                R.id.nav_sync -> {
                    dataSynchronizer?.showSyncOptionsDialog() ?: Toast.makeText(activity, "Data Synchronizer is not available here.", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_add_hive -> {
                    (activity as? MainActivity)?.showAddHiveDialog() ?: Toast.makeText(activity, "Cannot add hive from this screen.", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    // Додаємо нову функцію
    private fun deselectAllMenuItems(navView: NavigationView) {
        val menu: Menu = navView.menu
        for (i in 0 until menu.size()) {
            val menuItem: MenuItem = menu.getItem(i)
            menuItem.isChecked = false
        }
    }
}