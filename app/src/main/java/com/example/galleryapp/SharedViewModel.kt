package com.example.galleryapp

import android.content.Context
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private var categories: MutableList<Category> = mutableListOf()

    fun getCategories(): List<Category> = categories.toList()

    fun setCategories(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
    }

    fun addCategory(category: Category) {
        if (categories.none { it.id == category.id }) {
            categories.add(category)
        }
    }

    fun removeCategory(category: Category) {
        categories.removeIf { it.id == category.id }
    }

    fun updateCategory(updatedCategory: Category) {
        val index = categories.indexOfFirst { it.id == updatedCategory.id }
        if (index != -1) categories[index] = updatedCategory
    }

    fun updateCategoriesFromStorage(context: Context) {
        val storedCategories = CategoryStorage.loadCategories(context)
        setCategories(storedCategories)
    }

    fun saveCategoriesToStorage(context: Context) {
        CategoryStorage.saveCategories(context, categories)
    }
}