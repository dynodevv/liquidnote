package com.liquidnote.app.model

enum class BlockType {
    PARAGRAPH,
    HEADING1,
    HEADING2,
    HEADING3,
    BULLET,
    NUMBERED,
    QUOTE,
    CHECKBOX,
    CODE
}

fun BlockType.displayName(): String = when (this) {
    BlockType.PARAGRAPH -> "Paragraph"
    BlockType.HEADING1 -> "Heading 1"
    BlockType.HEADING2 -> "Heading 2"
    BlockType.HEADING3 -> "Heading 3"
    BlockType.BULLET -> "Bullet List"
    BlockType.NUMBERED -> "Numbered List"
    BlockType.QUOTE -> "Quote"
    BlockType.CHECKBOX -> "Checkbox"
    BlockType.CODE -> "Code Block"
}
