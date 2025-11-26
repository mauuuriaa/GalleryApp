package com.example.galleryapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.io.*

class MainActivity : BaseActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private val privacyLevels = listOf("Публичная", "Приватная", "Секретная")

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var categoryAdapter: CategoryAdapter

    // SAF для создания XLS
    private val createXlsLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.ms-excel")) { uri: Uri? ->
            uri?.let { exportCategoriesToXlsUri(it) } ?: showToast("Экспорт в XLS отменён")
        }

    // SAF для создания PDF
    private val createPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
            uri?.let { exportCategoriesToPdfUri(it) } ?: showToast("Экспорт в PDF отменён")
        }

    // SAF для выбора файлов (Excel/CSV)
    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { importFromFile(it) } ?: showToast("Выбор файла отменён")
        }

    // Лаунчер для AddCategoryActivity
    private val addCategoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val newCategory = data?.getSerializableExtra("newCategory") as? Category
            newCategory?.let {
                sharedViewModel.addCategory(it)
                sharedViewModel.saveCategoriesToStorage(this)
                refreshRecyclerView()
                showToast("Добавлена категория: ${it.name}")
            }
        }
    }

    override fun getLayoutResId() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recyclerView = findViewById(R.id.recyclerViewCategories)
        fab = findViewById(R.id.fabAddCategory)

        setupRecyclerView()
        setupSwipeToDelete()
        setupFab()
        setupDrawerMenuActions()

        sharedViewModel.updateCategoriesFromStorage(this)
        refreshRecyclerView()
    }

    private fun setupFab() {
        fab.setOnClickListener { launchAddCategoryActivity() }
    }

    private fun setupDrawerMenuActions() {
        // Главная
        navigationView.menu.findItem(R.id.nav_main).setOnMenuItemClickListener {
            drawerLayout.closeDrawers()
            true
        }

        // Добавить категорию
        navigationView.menu.findItem(R.id.nav_add_category).setOnMenuItemClickListener {
            launchAddCategoryActivity()
            drawerLayout.closeDrawers()
            true
        }

        // Импорт данных
        navigationView.menu.findItem(R.id.nav_import).setOnMenuItemClickListener {
            showImportDialog()
            drawerLayout.closeDrawers()
            true
        }

        // Экспорт в XLS
        navigationView.menu.findItem(R.id.nav_export_xls).setOnMenuItemClickListener {
            createXlsLauncher.launch("categories.xls")
            drawerLayout.closeDrawers()
            true
        }

        // Экспорт в PDF
        navigationView.menu.findItem(R.id.nav_export_pdf).setOnMenuItemClickListener {
            createPdfLauncher.launch("categories.pdf")
            drawerLayout.closeDrawers()
            true
        }
    }

    // Диалог выбора типа импорта
    private fun showImportDialog() {
        val importTypes = arrayOf("Из Excel (.xls, .csv)", "Загрузить из бинарного файла")

        AlertDialog.Builder(this)
            .setTitle("Импорт категорий")
            .setItems(importTypes) { dialog, which ->
                when (which) {
                    0 -> pickFileLauncher.launch("application/vnd.ms-excel") // Excel/CSV
                    1 -> loadFromInternalBinaryStorage() // Бинарный файл из внутреннего хранилища
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Загрузка из внутреннего бинарного хранилища
    private fun loadFromInternalBinaryStorage() {
        try {
            val categories = CategoryStorage.loadCategories(this)

            if (categories.isNotEmpty()) {
                sharedViewModel.setCategories(categories)
                refreshRecyclerView()
                showToast("Загружено ${categories.size} категорий из бинарного файла")
            } else {
                showToast("Бинарный файл не содержит категорий или не найден")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Internal binary load error: ${e.message}", e)
            showToast("Ошибка загрузки из бинарного файла")
        }
    }

    // Импорт из Excel/CSV файла
    private fun importFromFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileName(uri)

            when {
                fileName?.endsWith(".xls") == true || fileName?.endsWith(".csv") == true -> {
                    importFromExcel(inputStream)
                }
                else -> showToast("Неподдерживаемый формат файла")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Import error: ${e.message}", e)
            showToast("Ошибка импорта: ${e.message}")
        }
    }

    // Импорт из Excel/CSV
    private fun importFromExcel(inputStream: InputStream?) {
        try {
            inputStream?.use { stream ->
                val content = stream.bufferedReader().use { it.readText() }
                val lines = content.lines()

                if (lines.size < 2) {
                    showToast("Файл не содержит данных")
                    return
                }

                val importedCategories = mutableListOf<Category>()

                // Пропускаем заголовок и обрабатываем данные
                lines.drop(1).forEach { line ->
                    if (line.isNotBlank()) {
                        // Формат: id;"name";privacy
                        val parts = line.split(';')
                        if (parts.size >= 3) {
                            val id = parts[0].trim()
                            var name = parts[1].trim()

                            // Убираем кавычки если есть
                            if (name.startsWith("\"") && name.endsWith("\"")) {
                                name = name.substring(1, name.length - 1)
                            }

                            val privacy = parts[2].trim()

                            if (name.isNotEmpty()) {
                                importedCategories.add(Category(id = id, name = name, privacy = privacy))
                            }
                        }
                    }
                }

                if (importedCategories.isNotEmpty()) {
                    sharedViewModel.setCategories(importedCategories)
                    sharedViewModel.saveCategoriesToStorage(this)
                    refreshRecyclerView()
                    showToast("Импортировано ${importedCategories.size} категорий из Excel")
                } else {
                    showToast("Не удалось найти категории в файле")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Excel import error: ${e.message}", e)
            showToast("Ошибка импорта Excel файла")
        }
    }

    // Вспомогательная функция для получения имени файла
    private fun getFileName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        cursor.getString(displayNameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // Остальные методы без изменений...
    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { showEditDialog(it) }
        recyclerView.adapter = categoryAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 3, RecyclerView.HORIZONTAL, false)
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP or ItemTouchHelper.DOWN) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val category = categoryAdapter.currentList[viewHolder.adapterPosition]
                sharedViewModel.removeCategory(category)
                sharedViewModel.saveCategoriesToStorage(this@MainActivity)
                refreshRecyclerView()
                Snackbar.make(recyclerView, "Категория \"${category.name}\" удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {
                        sharedViewModel.addCategory(category)
                        sharedViewModel.saveCategoriesToStorage(this@MainActivity)
                        refreshRecyclerView()
                    }.show()
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun showEditDialog(categoryToEdit: Category) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_category, null)
        val editName = view.findViewById<EditText>(R.id.editTextEditName)
        val spinnerPrivacy = view.findViewById<Spinner>(R.id.spinnerEditPrivacy)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, privacyLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrivacy.adapter = adapter

        editName.setText(categoryToEdit.name)
        spinnerPrivacy.setSelection(privacyLevels.indexOf(categoryToEdit.privacy).coerceAtLeast(0))

        AlertDialog.Builder(this)
            .setTitle("Редактировать категорию")
            .setView(view)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = editName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updated = categoryToEdit.copy(name = newName, privacy = spinnerPrivacy.selectedItem.toString())
                    sharedViewModel.updateCategory(updated)
                    sharedViewModel.saveCategoriesToStorage(this)
                    refreshRecyclerView()
                    dialog.dismiss()
                } else showToast("Имя не может быть пустым")
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun launchAddCategoryActivity() {
        val intent = Intent(this, AddCategoryActivity::class.java)
        addCategoryLauncher.launch(intent)
    }

    private fun refreshRecyclerView() {
        categoryAdapter.submitList(sharedViewModel.getCategories())
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.updateCategoriesFromStorage(this)
        refreshRecyclerView()
    }

    // --- Export XLS ---
    private fun exportCategoriesToXlsUri(uri: Uri) {
        try {
            val categories = sharedViewModel.getCategories()
            if (categories.isEmpty()) { showToast("Нет категорий для экспорта"); return }

            val csv = buildString {
                append("id;name;privacy\n")  // используем ; как разделитель
                categories.forEach { c ->
                    val safeName = c.name.replace("\"", "\"\"")
                    append("${c.id};\"$safeName\";${c.privacy}\n")
                }
            }

            // Добавляем BOM для корректного отображения UTF-8
            val bom = "\uFEFF".toByteArray(Charsets.UTF_8)
            contentResolver.openOutputStream(uri)?.use {
                it.write(bom)
                it.write(csv.toByteArray(Charsets.UTF_8))
            }

            showToast("Экспорт в XLS выполнен")
        } catch (e: Exception) {
            Log.e("MainActivity", "XLS export error: ${e.message}", e)
            showToast("Ошибка при экспорте XLS")
        }
    }

    // --- Export PDF ---
    private fun exportCategoriesToPdfUri(uri: Uri) {
        try {
            val categories = sharedViewModel.getCategories()
            if (categories.isEmpty()) { showToast("Нет категорий для экспорта"); return }

            val pageWidth = 595; val pageHeight = 842
            val document = PdfDocument()
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas
            val paint = Paint().apply { textSize = 12f }

            var y = 40f
            paint.isFakeBoldText = true
            canvas.drawText("Список категорий", 20f, y, paint)
            paint.isFakeBoldText = false
            y += 24f
            canvas.drawText("ID", 20f, y, paint)
            canvas.drawText("Name", 220f, y, paint)
            canvas.drawText("Privacy", 460f, y, paint)
            y += 18f

            categories.forEach { c ->
                if (y > pageHeight - 40) return@forEach
                val displayName = if (c.name.length > 30) c.name.substring(0, 27) + "..." else c.name
                canvas.drawText(c.id.take(20), 20f, y, paint)
                canvas.drawText(displayName, 220f, y, paint)
                canvas.drawText(c.privacy, 460f, y, paint)
                y += 16f
            }

            document.finishPage(page)
            contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
            showToast("Экспорт в PDF выполнен")
        } catch (e: Exception) {
            Log.e("MainActivity", "PDF export error: ${e.message}", e)
            showToast("Ошибка при экспорте PDF")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}