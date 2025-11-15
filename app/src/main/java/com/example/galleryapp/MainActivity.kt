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

    // Лаунчер для приёма результата от AddCategoryActivity
    private val addCategoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // Получаем новую категорию (с новым ID, созданным в AddCategoryActivity)
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

    override fun onSetupDrawerMenu() {
        recyclerView = findViewById(R.id.recyclerViewCategories)
        fab = findViewById(R.id.fabAddCategory)

        // 1. Настройка RecyclerView (Read)
        setupRecyclerView()

        // 2. Настройка FAB (Create)
        fab.setOnClickListener {
            launchAddCategoryActivity()
        }

        // 3. Настройка Swipe-to-Delete (Delete)
        setupSwipeToDelete()

        // 4. Загрузка данных
        sharedViewModel.updateCategoriesFromStorage(this)
        refreshRecyclerView()

        // (Опционально) Оставляем добавление и через боковое меню
        val menu = navigationView.menu.findItem(R.id.nav_add_category)
        menu.setOnMenuItemClickListener {
            launchAddCategoryActivity()
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupRecyclerView() {
        // Создаем адаптер и передаем ему лямбду для обработки "Update"
        categoryAdapter = CategoryAdapter { category ->
            showEditDialog(category)
        }

        recyclerView.adapter = categoryAdapter

        // Устанавливаем LayoutManager согласно требованиям:
        // Горизонтальная ориентация (RecyclerView.HORIZONTAL)
        // 3 элемента в столбце (spanCount = 3)
        val layoutManager = GridLayoutManager(this, 3, RecyclerView.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            // dragDirs (Перетаскивание): 0 (не используется)
            0,
            // swipeDirs (Свайп): Разрешаем свайп вверх ИЛИ вниз
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Перетаскивание не используется
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val category = categoryAdapter.currentList[position]

                // 1. Удаляем из ViewModel и Storage
                sharedViewModel.removeCategory(category)
                sharedViewModel.saveCategoriesToStorage(this@MainActivity)

                // 2. Обновляем UI
                refreshRecyclerView()

                // 3. Показываем Snackbar с возможностью отмены
                Snackbar.make(recyclerView, "Категория \"${category.name}\" удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {
                        // Если отмена - возвращаем категорию
                        sharedViewModel.addCategory(category)
                        sharedViewModel.saveCategoriesToStorage(this@MainActivity)
                        refreshRecyclerView()
                    }
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showEditDialog(categoryToEdit: Category) {
        // (Update)
        val view = layoutInflater.inflate(R.layout.dialog_edit_category, null)
        val editName = view.findViewById<EditText>(R.id.editTextEditName)
        val spinnerPrivacy = view.findViewById<Spinner>(R.id.spinnerEditPrivacy)

        // Адаптер для спиннера
        val privacyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, privacyLevels)
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrivacy.adapter = privacyAdapter

        // Заполняем диалог текущими данными
        editName.setText(categoryToEdit.name)
        val currentPrivacyIndex = privacyLevels.indexOf(categoryToEdit.privacy)
        if (currentPrivacyIndex >= 0) {
            spinnerPrivacy.setSelection(currentPrivacyIndex)
        }

        // Показываем диалог
        AlertDialog.Builder(this)
            .setTitle("Редактировать категорию")
            .setView(view)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = editName.text.toString().trim()
                val newPrivacy = spinnerPrivacy.selectedItem.toString()

                if (newName.isEmpty()) {
                    Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Создаем обновленный объект (ID остается тот же!)
                val updatedCategory = categoryToEdit.copy(
                    name = newName,
                    privacy = newPrivacy
                )

                // Обновляем в ViewModel и Storage
                sharedViewModel.updateCategory(updatedCategory)
                sharedViewModel.saveCategoriesToStorage(this)
                refreshRecyclerView()

                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun launchAddCategoryActivity() {
        val intent = Intent(this, AddCategoryActivity::class.java)
        addCategoryLauncher.launch(intent)
    }

    private fun refreshRecyclerView() {
        // Передаем обновленный список в ListAdapter. DiffUtil сделает все остальное.
        val categories = sharedViewModel.getCategories()
        categoryAdapter.submitList(categories)
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран (например, после AddCategoryActivity)
        sharedViewModel.updateCategoriesFromStorage(this)
        refreshRecyclerView()
    }
}
