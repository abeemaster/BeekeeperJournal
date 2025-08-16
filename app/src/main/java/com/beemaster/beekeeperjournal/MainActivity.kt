// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType

import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var hiveRepository: HiveRepository
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var hiveListRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView
    private lateinit var dataSynchronizer: DataSynchronizer


    companion object {
        const val EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE = "com.beemaster.beekeeperjournal.HIVE_NUMBER_FOR_COLOR_UPDATE"

    }

    private val colorPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedColor = result.data?.getIntExtra("selected_color", Color.TRANSPARENT)
            val hiveNumber = result.data?.getIntExtra("hive_number", -1)
            val colorType = result.data?.getStringExtra("color_type")

            if (hiveNumber != -1 && selectedColor != null && colorType != null) {
                val hives = hiveRepository.readHivesFromJson()
                val hiveToUpdate = hives.find { it.number == hiveNumber }
                if (hiveToUpdate != null) {
                    when (colorType) {
                        "primary" -> hiveToUpdate.color = selectedColor
                        "secondary" -> hiveToUpdate.secondaryColor = selectedColor
                    }
                    hiveRepository.writeHivesToJson(hives)
                    loadHives()
                }
            }
        }
    }

    private val createBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                dataSynchronizer.writeBackupDataToFile(uri)
            }
        }
    }

    private val openBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                dataSynchronizer.readAndRestoreBackupDataFromFile(uri)
            }
        }
    }

    private val pickFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                dataSynchronizer.exportNotesToCsvFiles(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hiveRepository = HiveRepository(this)

        dataSynchronizer = DataSynchronizer(
            this,
            createBackupFileLauncher,
            openBackupFileLauncher,
            pickFolderLauncher
        ) { loadHives() }

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val drawerToggleButton: ImageButton = findViewById(R.id.drawer_toggle_button)

        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        hiveListRecyclerView = findViewById(R.id.hiveListRecyclerView)
        hiveCountTextView = findViewById(R.id.hiveCountTextView)
        hiveListRecyclerView.layoutManager = LinearLayoutManager(this)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Ми вже на головному екрані, тому нічого не робимо.
                }
                R.id.nav_general_notes -> {
                    val intent = Intent(this, HiveInfoActivity::class.java).apply {
                        putExtra("TYPE", "general")
                        putExtra("TITLE", "Загальні записи")
                        putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, "Загальні записи")
                    }
                    startActivity(intent)
                }
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_sync -> {
                    dataSynchronizer.showSyncOptionsDialog()
                }
                R.id.nav_add_hive -> {
                    showAddHiveDialog()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val generalNotesBtn: MaterialButton = findViewById(R.id.generalNotesButton)
        generalNotesBtn.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "general")
                putExtra("TITLE", "Загальні записи")
                putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, "Загальні записи")
            }
            startActivity(intent)
        }

        val hives = hiveRepository.readHivesFromJson()
        hiveAdapter = HiveAdapter(
            hives = hives,
            onItemClick = { position ->
                val hive = hives[position]
                val intent = Intent(this, HiveInfoActivity::class.java).apply {
                    putExtra("HIVE_NUMBER", hive.number)
                    putExtra("TYPE", "hive")
                }
                startActivity(intent)
            },
            onItemLongClick = { position ->
                val hive = hives[position]
                showHiveOptionsDialog(hive)
            }
        )
        hiveListRecyclerView.adapter = hiveAdapter
        updateHiveCount(hives.size)
    }

    override fun onResume() {
        super.onResume()
        loadHives()
    }

    private fun loadHives() {
        val hives = hiveRepository.readHivesFromJson()
        if (hives.isEmpty()) {
            for (i in 1..30) {
                hives.add(HiveData(
                    number = i,
                    name = "Вулик №$i",
                    color = R.color.hive_button_color,
                    queenButtonColor = R.color.nav_button_color,
                    notesButtonColor = R.color.nav_button_color,
                    secondaryColor = android.R.color.transparent
                ))
            }
            hiveRepository.writeHivesToJson(hives)
        }
        hiveAdapter.updateHives(hives)
        updateHiveCount(hives.size)
    }

    @SuppressLint("SetTextI18n")
    private fun updateHiveCount(count: Int) {
        val countString = when (count) {
            1 -> "вулик"
            in 2..4 -> "вулики"
            else -> "вуликів"
        }
        hiveCountTextView.text = "$count $countString"
    }

    private fun showAddHiveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Додати новий вулик")
        val input = EditText(this).apply { hint = "Назва вулика"; inputType = InputType.TYPE_CLASS_TEXT }
        builder.setView(input)

        builder.setPositiveButton("Додати") { dialog, _ ->
            val hiveName = input.text.toString().trim()
            if (hiveName.isNotEmpty()) {
                val hives = hiveRepository.readHivesFromJson()
                val newHiveNumber = (hives.maxByOrNull { it.number }?.number ?: 0) + 1
                hives.add(HiveData(
                    number = newHiveNumber,
                    name = hiveName,
                    color = R.color.hive_button_color,
                    queenButtonColor = R.color.nav_button_color,
                    notesButtonColor = R.color.nav_button_color,
                    secondaryColor = android.R.color.transparent
                ))
                hiveRepository.writeHivesToJson(hives)
                loadHives()
                (dialog as AlertDialog).dismiss()
            } else {
                Toast.makeText(this, "Назва вулика не може бути порожньою", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Скасувати") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showHiveOptionsDialog(hive: HiveData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_hive_options, null)
        val dialogTitleTextView: TextView = dialogView.findViewById(R.id.dialogTitle)
        val editNameCard: MaterialCardView = dialogView.findViewById(R.id.editNameCard)
        val selectPrimaryColorCard: MaterialCardView = dialogView.findViewById(R.id.selectPrimaryColorCard)
        val selectSecondaryColorCard: MaterialCardView = dialogView.findViewById(R.id.selectSecondaryColorCard)
        val deleteHiveCard: MaterialCardView = dialogView.findViewById(R.id.deleteHiveCard)

        dialogTitleTextView.text = getString(R.string.hive_options_title, hive.name)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        editNameCard.setOnClickListener {
            dialog.dismiss()
            showEditHiveNameDialog(hive)
        }
        selectPrimaryColorCard.setOnClickListener {
            dialog.dismiss()
            openColorPicker(hive.number, "primary")
        }
        selectSecondaryColorCard.setOnClickListener {
            dialog.dismiss()
            openColorPicker(hive.number, "secondary")
        }
        deleteHiveCard.setOnClickListener {
            dialog.dismiss()
            showDeleteHiveDialog(hive)
        }
        dialog.show()
    }

    private fun showEditHiveNameDialog(hive: HiveData) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Редагувати назву вулика")
        val input = EditText(this).apply {
            setText(hive.name)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        builder.setView(input)
        builder.setPositiveButton("Зберегти") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty() && newName != hive.name) {
                val hives = hiveRepository.readHivesFromJson()
                hives.find { it.number == hive.number }?.name = newName
                hiveRepository.writeHivesToJson(hives)
                loadHives()
            } else {
                Toast.makeText(this, "Назва не може бути порожньою", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Скасувати") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showDeleteHiveDialog(hive: HiveData) {
        AlertDialog.Builder(this)
            .setTitle("Видалити вулик")
            .setMessage("Ви впевнені, що хочете видалити вулик №${hive.number} (${hive.name})? Всі пов'язані з ним записи також будуть видалені.")
            .setPositiveButton("Видалити") { dialog, _ ->
                val hives = hiveRepository.readHivesFromJson().filter { it.number != hive.number }.toMutableList()
                hiveRepository.writeHivesToJson(hives)
                loadHives()
                val notes = dataSynchronizer.readAllNotesFromJson().filter { it.hiveNumber != hive.number }.toMutableList()
                dataSynchronizer.writeAllNotesToJson(notes)
                Toast.makeText(this, "Вулик ${hive.name} видалено.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun openColorPicker(hiveNumber: Int, colorType: String) {
        val intent = Intent(this, ColorPickerActivity::class.java).apply {
            putExtra("hive_number", hiveNumber)
            putExtra("color_type", colorType)
        }
        colorPickerLauncher.launch(intent)
    }
}