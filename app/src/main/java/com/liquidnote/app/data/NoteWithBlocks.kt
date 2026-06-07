package com.liquidnote.app.data

data class NoteWithBlocks(
    val note: NoteEntity,
    val blocks: List<BlockEntity>
)
