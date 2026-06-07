package com.liquidnote.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val type: String = "paragraph",
    val content: String = "",
    val orderIndex: Int = 0,
    val isChecked: Boolean = false
)
