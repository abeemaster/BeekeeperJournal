// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import db.HiveEntity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var hiveListRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView
    private lateinit var dataSynchronizer: DataSynchronizer
    private lateinit var pickFolderLauncher: ActivityResultLauncher<Uri?>

    private lateinit var createBackupFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var openBackupFileLauncher: ActivityResultLauncher<Intent>

    private lateinit var colorPickerLauncher: ActivityResultLauncher<Intent>

    private val hiveRepository: HiveRepository by lazy {
        (application as BeekeeperApplication).hiveRepository
    }

    companion object {
        const val EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE = "com.beemaster.beekeeperjournal.HIVE_NUMBER_FOR_COLOR_UPDATE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        pickFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) { // ✅ ДОДАНО: Перевірка на null
               //ataSynchronizer.exportNotesToCsvFiles(uri)
            }
        }




        createBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    lifecycleScope.launch {
                        dataSynchronizer.writeBackupDataToFile(uri)
                    }
                }
            }
        }

        openBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    lifecycleScope.launch {
                        dataSynchronizer.readAndRestoreBackupDataFromFile(uri) { loadHives() }
                        loadHives()
                    }
                }
            }
        }

        colorPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedColor = result.data?.getIntExtra("selected_color", Color.TRANSPARENT)
                val hiveNumber = result.data?.getIntExtra("hive_number", -1)
                val colorType = result.data?.getStringExtra("color_type")

                if (hiveNumber != -1 && selectedColor != null && colorType != null) {
                    lifecycleScope.launch {
                        val hiveToUpdate = hiveRepository.getHiveByNumber(hiveNumber!!)
                        if (hiveToUpdate != null) {
                            val updatedHive = when (colorType) {
                                "primary" -> hiveToUpdate.copy(color = selectedColor)
                                "secondary" -> hiveToUpdate.copy(secondaryColor = selectedColor)
                                else -> hiveToUpdate
                            }
                            hiveRepository.updateHive(updatedHive)
                            loadHives()
                        }
                    }
                }
            }
        }

        dataSynchronizer = DataSynchronizer(
            this,
            createBackupFileLauncher,
            openBackupFileLauncher,
            pickFolderLauncher
        )
        { loadHives() }

        DrawerManager.setupDrawer(this, dataSynchronizer)

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

    fun showAddHiveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Додати новий вулик")
        val input = EditText(this).apply { hint = "Назва вулика"; inputType = InputType.TYPE_CLASS_TEXT }
        builder.setView(input)
        builder.setPositiveButton("Додати") { dialog, _ ->
            val hiveName = input.text.toString().trim()
            if (hiveName.isNotEmpty()) {
                lifecycleScope.launch {
                    val hives = hiveRepository.getAllHives()
                    val newHiveNumber = (hives.maxByOrNull { it.hiveNumber }?.hiveNumber ?: 0) + 1

                    val newHive = HiveEntity(
                        hiveNumber = newHiveNumber,
                        name = hiveName,
                    )
                    hiveRepository.insertHive(newHive)
                    loadHives()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Назва вулика не може бути порожньою", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Скасувати") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun loadHives() {
        lifecycleScope.launch {
            var hives = hiveRepository.getAllHives().toMutableList()

            if (hives.isEmpty()) {
                for (i in 1..30) {
                    val newHive = HiveEntity(hiveNumber = i, name = "Вулик №$i")
                    hiveRepository.insertHive(newHive)
                }
                hives = hiveRepository.getAllHives().toMutableList()
            }

            hiveCountTextView.text = getString(R.string.hive_count, hives.size)

            hiveAdapter = HiveAdapter(hives.toMutableList(), this@MainActivity) { position ->
                showHiveOptionsDialog(hives[position])
            }
            hiveListRecyclerView.adapter = hiveAdapter
        }
    }

    private fun showHiveOptionsDialog(hive: HiveEntity) {
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
            openColorPicker(hive.hiveNumber, "primary")
        }
        selectSecondaryColorCard.setOnClickListener {
            dialog.dismiss()
            openColorPicker(hive.hiveNumber, "secondary")
        }
        deleteHiveCard.setOnClickListener {
            dialog.dismiss()
            showDeleteHiveDialog(hive)
        }

        dialog.show()
    }

    private fun showEditHiveNameDialog(hive: HiveEntity) {
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
                lifecycleScope.launch {
                    val updatedHive = hive.copy(name = newName)
                    hiveRepository.updateHive(updatedHive)
                    loadHives()
                }
            } else {
                Toast.makeText(this, "Назва не може бути порожньою", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Скасувати") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showDeleteHiveDialog(hive: HiveEntity) {
        AlertDialog.Builder(this)
            .setTitle("Видалити вулик")
            .setMessage("Ви впевнені, що хочете видалити вулик №${hive.hiveNumber} (${hive.name})? Всі пов'язані з ним записи також будуть видалені.")
            .setPositiveButton("Видалити") { dialog, _ ->
                lifecycleScope.launch {
                    // Видаляємо вулик
                    hiveRepository.deleteHive(hive.hiveNumber)
                    // Видаляємо всі нотатки, пов'язані з цим вуликом
                    hiveRepository.getNotesByHiveNumber(hive.hiveNumber).forEach { note ->
                        hiveRepository.deleteNote(note.id)
                    }
                    loadHives()
                    Toast.makeText(this@MainActivity, "Вулик ${hive.name} видалено.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
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