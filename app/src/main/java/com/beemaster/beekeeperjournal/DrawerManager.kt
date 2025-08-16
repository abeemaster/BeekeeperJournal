// DrawerManager файл що відповідає за роботу бічної панелі

package com.beemaster.beekeeperjournal

import android.content.Intent
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.activity.OnBackPressedCallback
import android.widget.Toast

object DrawerManager {

    fun setupDrawer(activity: AppCompatActivity, dataSynchronizer: DataSynchronizer? = null) {
        val drawerLayout: DrawerLayout = activity.findViewById(R.id.drawer_layout)
        val navView: NavigationView = activity.findViewById(R.id.nav_view)
        val drawerToggleButton: ImageButton = activity.findViewById(R.id.drawer_toggle_button)

        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Обробка зворотного натискання (закриття панелі)
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

        // Обробник натискання на пункти меню
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(activity, MainActivity::class.java)
                    activity.startActivity(intent)
                }
                R.id.nav_general_notes -> {
                    val intent = Intent(activity, HiveInfoActivity::class.java).apply {
                        putExtra("TYPE", "general")
                        putExtra("TITLE", "Загальні записи")
                        putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, "Загальні записи")
                    }
                    activity.startActivity(intent)
                }
                R.id.nav_search -> {
                    val intent = Intent(activity, SearchActivity::class.java)
                    activity.startActivity(intent)
                }
                R.id.nav_sync -> {
                    dataSynchronizer?.showSyncOptionsDialog() ?: Toast.makeText(activity, "Data Synchronizer is not available here.", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_add_hive -> {
                    (activity as? MainActivity)?.showAddHiveDialog() ?: Toast.makeText(activity, "Cannot add hive from this screen.", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}