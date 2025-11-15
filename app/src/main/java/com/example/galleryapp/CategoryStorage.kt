package com.example.galleryapp

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CategoryStorage {
    private const val PREFS_NAME = "categories_prefs"
    private const val KEY_CATEGORIES = "categories_list"

    fun saveCategories(context: Context, categories: List<Category>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(categories)
            prefs.edit().putString(KEY_CATEGORIES, json).apply()
            Log.d("CategoryStorage", "Saved ${categories.size} categories to storage")
        } catch (e: Exception) {
            Log.e("CategoryStorage", "Error saving categories: ${e.message}")
        }
    }

    fun loadCategories(context: Context): MutableList<Category> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_CATEGORIES, null)
            if (json.isNullOrEmpty()) {
                Log.d("CategoryStorage", "No categories found in storage, returning empty list")
                mutableListOf()
            } else {
                val type = object : TypeToken<MutableList<Category>>() {}.type
                val categories = Gson().fromJson<MutableList<Category>>(json, type) ?: mutableListOf()
                Log.d("CategoryStorage", "Loaded ${categories.size} categories from storage")
                categories
            }
        } catch (e: Exception) {
            Log.e("CategoryStorage", "Error loading categories: ${e.message}")
            mutableListOf()
        }
    }
}