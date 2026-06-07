package com.liquidnote.app.model

data class Category(
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF007AFF.toInt()
)
