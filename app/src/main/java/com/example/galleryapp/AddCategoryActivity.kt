package com.example.galleryapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.android.material.snackbar.Snackbar

class AddCategoryActivity : BaseActivity() {

    private lateinit var editTextCategoryName: EditText
    private lateinit var spinnerPrivacy: Spinner
    private lateinit var buttonSaveCategory: Button

    private val privacyLevels = listOf("Публичная", "Приватная", "Секретная")

    override fun getLayoutResId() = R.layout.activity_add_category

    override fun onSetupDrawerMenu() {
        editTextCategoryName = findViewById(R.id.editTextCategoryName)
        spinnerPrivacy = findViewById(R.id.spinnerPrivacy)
        buttonSaveCategory = findViewById(R.id.buttonSaveCategory)

        val privacyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, privacyLevels)
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrivacy.adapter = privacyAdapter

        buttonSaveCategory.setOnClickListener {
            try {
                val name = editTextCategoryName.text.toString().trim()
                val privacy = spinnerPrivacy.selectedItem.toString()

                if (name.isEmpty()) {
                    Snackbar.make(buttonSaveCategory, "Введите название категории", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // ----- ВОТ ИСПРАВЛЕНИЕ -----
                // Используем именованные аргументы, чтобы id сгенерировался по умолчанию
                val newCategory = Category(name = name, privacy = privacy)
                // -------------------------

                Log.d("AddCategory", "Category created: $name ($privacy)")

                //  Возвращаем категорию обратно в MainActivity
                val resultIntent = Intent().apply {
                    putExtra("newCategory", newCategory)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()

            } catch (e: Exception) {
                Log.e("AddCategory", "Error saving category: ${e.message}", e)
                Snackbar.make(buttonSaveCategory, "Ошибка при сохранении категории", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}