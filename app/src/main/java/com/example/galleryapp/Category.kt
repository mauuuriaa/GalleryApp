package com.example.galleryapp

import java.io.Serializable
import java.util.UUID

data class Category(
    val id: String = UUID.randomUUID().toString(), // Добавляем уникальный ID
    var name: String,
    var privacy: String
) : Serializable