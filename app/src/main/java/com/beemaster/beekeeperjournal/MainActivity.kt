// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var hiveRepository: HiveRepository
    private lateinit var hiveListRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView
    private lateinit var dataSynchronizer: DataSynchronizer

    // ✅ Оголошення лаунчерів. Вони тут лише оголошуються!
    private lateinit var pickFolderLauncher: ActivityResultLauncher<Intent>
    private lateinit var createBackupFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var openBackupFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var colorPickerLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE = "com.beemaster.beekeeperjournal.HIVE_NUMBER_FOR_COLOR_UPDATE"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ КРОК 1: Ініціалізуємо усі лаунчери
        pickFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    dataSynchronizer.exportNotesToCsvFiles(uri)
                }
            }
        }

        createBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    dataSynchronizer.writeBackupDataToFile(uri)
                }
            }
        }

        openBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    dataSynchronizer.readAndRestoreBackupDataFromFile(uri) { loadHives() }
                }
            }
        }

        colorPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        // ✅ КРОК 2: Тільки тепер ініціалізуємо DataSynchronizer, використовуючи лаунчери, що вже існують
        dataSynchronizer = DataSynchronizer(
            this,
            createBackupFileLauncher,
            openBackupFileLauncher,
            pickFolderLauncher
        )

        // ✅ КРОК 3: Викликаємо setupDrawer лише один раз
        DrawerManager.setupDrawer(this, dataSynchronizer)


        hiveRepository = HiveRepository(this)


        hiveListRecyclerView = findViewById(R.id.hiveListRecyclerView)
        hiveCountTextView = findViewById(R.id.hiveCountTextView)
        hiveListRecyclerView.layoutManager = LinearLayoutManager(this)


        val generalNotesBtn: MaterialButton = findViewById(R.id.generalNotesButton)
        loadHives()
        generalNotesBtn.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "general")
                putExtra("TITLE", "Загальні записи")
                putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, "Загальні записи")
            }
            startActivity(intent)
        }
    }

    // ... Інші методи (showAddHiveDialog, loadHives, showHiveOptionsDialog, тощо)
    // які не відносяться до синхронізації...

    fun showAddHiveDialog() {
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
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Назва вулика не може бути порожньою", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Скасувати") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // ... інші методи, які не належать до синхронізації...
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

        hiveCountTextView.text = getString(R.string.hive_count, hives.size)

        hiveAdapter = HiveAdapter(hives, this) { position ->
            showHiveOptionsDialog(hives[position])
        }
        hiveListRecyclerView.adapter = hiveAdapter
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