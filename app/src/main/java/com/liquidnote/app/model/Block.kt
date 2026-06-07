package com.liquidnote.app.model

data class Block(
    val id: Long = 0,
    val noteId: Long,
    val type: BlockType = BlockType.PARAGRAPH,
    val content: String = "",
    val order: Int = 0,
    val isChecked: Boolean = false
)
