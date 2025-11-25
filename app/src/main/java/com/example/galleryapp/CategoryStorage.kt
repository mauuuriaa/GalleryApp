package com.example.galleryapp

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
object CategoryStorage {
    private const val BIN_FILENAME = "categories.bin"

    fun saveCategories(context: Context, categories: List<Category>) {
        try {
            val file = File(context.filesDir, BIN_FILENAME)
            ObjectOutputStream(FileOutputStream(file)).use { it.writeObject(ArrayList(categories)) }
            Log.d("CategoryStorage", "Saved ${categories.size} categories to binary file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("CategoryStorage", "Error saving categories: ${e.message}", e)
        }
    }

    fun loadCategories(context: Context): MutableList<Category> {
        return try {
            val file = File(context.filesDir, BIN_FILENAME)
            if (!file.exists()) {
                Log.d("CategoryStorage", "Binary file not found, returning empty list")
                mutableListOf()
            } else {
                @Suppress("UNCHECKED_CAST")
                val list = ObjectInputStream(FileInputStream(file)).use { it.readObject() as? MutableList<Category> }
                Log.d("CategoryStorage", "Loaded ${list?.size ?: 0} categories from binary file")
                list ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e("CategoryStorage", "Error loading categories: ${e.message}", e)
            mutableListOf()
        }
    }

    fun deleteBinaryFile(context: Context) {
        try {
            val file = File(context.filesDir, BIN_FILENAME)
            if (file.exists() && file.delete()) Log.d("CategoryStorage", "Binary file deleted")
        } catch (e: Exception) {
            Log.e("CategoryStorage", "Error deleting binary file: ${e.message}", e)
        }
    }
}
