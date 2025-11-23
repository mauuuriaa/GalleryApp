package com.example.galleryapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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

class MainActivity : BaseActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private val privacyLevels = listOf("Публичная", "Приватная", "Секретная")

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var categoryAdapter: CategoryAdapter

    // Лаунчер для получения результата из AddCategoryActivity
    private val addCategoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val newCategory = data?.getSerializableExtra("newCategory") as? Category
            if (newCategory != null) {
                sharedViewModel.addCategory(newCategory)
                sharedViewModel.saveCategoriesToStorage(this)
                refreshRecyclerView()
                Toast.makeText(this, "Добавлена категория: ${newCategory.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getLayoutResId() = R.layout.activity_main


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация View
        recyclerView = findViewById(R.id.recyclerViewCategories)
        fab = findViewById(R.id.fabAddCategory)

        // 1. Настройка компонентов
        setupRecyclerView()
        setupSwipeToDelete()
        setupFab()
        setupCustomNavigationLogic() // Переопределяем логику меню для этой активити

        // 2. Загрузка данных
        sharedViewModel.updateCategoriesFromStorage(this)
        refreshRecyclerView()
    }

    private fun setupFab() {
        fab.setOnClickListener {
            launchAddCategoryActivity()
        }
    }

    // Переопределяем клик по меню только для этой активити,

    private fun setupCustomNavigationLogic() {
        val addMenuItem = navigationView.menu.findItem(R.id.nav_add_category)
        addMenuItem.setOnMenuItemClickListener {
            launchAddCategoryActivity()
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            showEditDialog(category)
        }
        recyclerView.adapter = categoryAdapter

        // SpanCount 3, горизонтальный скролл
        recyclerView.layoutManager = GridLayoutManager(this, 3, RecyclerView.HORIZONTAL, false)
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.UP or ItemTouchHelper.DOWN
        ) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val category = categoryAdapter.currentList[position]

                sharedViewModel.removeCategory(category)
                sharedViewModel.saveCategoriesToStorage(this@MainActivity)
                refreshRecyclerView()

                Snackbar.make(recyclerView, "Категория \"${category.name}\" удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {
                        sharedViewModel.addCategory(category)
                        sharedViewModel.saveCategoriesToStorage(this@MainActivity)
                        refreshRecyclerView()
                    }
                    .show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    private fun showEditDialog(categoryToEdit: Category) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_category, null)
        val editName = view.findViewById<EditText>(R.id.editTextEditName)
        val spinnerPrivacy = view.findViewById<Spinner>(R.id.spinnerEditPrivacy)

        val privacyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, privacyLevels)
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrivacy.adapter = privacyAdapter

        editName.setText(categoryToEdit.name)
        val currentPrivacyIndex = privacyLevels.indexOf(categoryToEdit.privacy)
        if (currentPrivacyIndex >= 0) spinnerPrivacy.setSelection(currentPrivacyIndex)

        AlertDialog.Builder(this)
            .setTitle("Редактировать категорию")
            .setView(view)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = editName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedCategory = categoryToEdit.copy(
                        name = newName,
                        privacy = spinnerPrivacy.selectedItem.toString()
                    )
                    sharedViewModel.updateCategory(updatedCategory)
                    sharedViewModel.saveCategoriesToStorage(this)
                    refreshRecyclerView()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                }
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
}