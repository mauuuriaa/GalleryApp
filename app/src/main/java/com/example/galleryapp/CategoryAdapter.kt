package com.example.galleryapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val onEditClick: (Category) -> Unit // Лямбда для обработки клика "Редактировать"
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category, onEditClick)
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener {

        private val nameTextView: TextView = itemView.findViewById(R.id.textViewCategoryName)
        private val privacyTextView: TextView = itemView.findViewById(R.id.textViewCategoryPrivacy)
        private lateinit var currentCategory: Category
        private lateinit var onEditClickCallback: (Category) -> Unit

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        fun bind(category: Category, onEditClick: (Category) -> Unit) {
            currentCategory = category
            onEditClickCallback = onEditClick
            nameTextView.text = category.name
            privacyTextView.text = category.privacy
        }

        // 1. Создание контекстного меню
        override fun onCreateContextMenu(
            menu: android.view.ContextMenu,
            v: View?,
            menuInfo: android.view.ContextMenu.ContextMenuInfo?
        ) {
            // Добавляем пункт "Редактировать"
            val editItem = menu.add(0, v!!.id, 0, "Редактировать")

            // 2. Устанавливаем слушатель на этот пункт
            editItem.setOnMenuItemClickListener {
                // 3. Вызываем лямбду, переданную в адаптер
                onEditClickCallback(currentCategory)
                true
            }
        }
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem
    }
}
