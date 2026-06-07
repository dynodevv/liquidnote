package com.liquidnote.app.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
