package com.liquidnote.app.repository

import com.liquidnote.app.data.AppDatabase
import com.liquidnote.app.data.BlockEntity
import com.liquidnote.app.data.CategoryEntity
import com.liquidnote.app.data.NoteEntity
import com.liquidnote.app.data.NoteWithBlocks
import com.liquidnote.app.model.Block
import com.liquidnote.app.model.BlockType
import com.liquidnote.app.model.Category
import com.liquidnote.app.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteRepository(private val db: AppDatabase) {

    // Categories
    fun getAllCategories(): Flow<List<Category>> =
        db.categoryDao().getAll().map { list -> list.map { it.toModel() } }

    suspend fun getCategoryById(id: Long): Category? =
        db.categoryDao().getById(id)?.toModel()

    suspend fun insertCategory(category: Category): Long =
        db.categoryDao().insert(category.toEntity())

    suspend fun updateCategory(category: Category) =
        db.categoryDao().update(category.toEntity())

    suspend fun deleteCategory(category: Category) =
        db.categoryDao().delete(category.toEntity())

    // Notes
    fun getAllNotes(): Flow<List<Note>> =
        db.noteDao().getAll().map { list -> list.map { it.toModel() } }

    fun getNotesByCategory(categoryId: Long): Flow<List<Note>> =
        db.noteDao().getByCategory(categoryId).map { list -> list.map { it.toModel() } }

    fun searchNotes(query: String): Flow<List<Note>> =
        db.noteDao().search(query).map { list -> list.map { it.toModel() } }

    suspend fun getNoteById(id: Long): Note? =
        db.noteDao().getById(id)?.toModel()

    suspend fun insertNote(note: Note): Long =
        db.noteDao().insert(note.toEntity())

    suspend fun updateNote(note: Note) =
        db.noteDao().update(note.toEntity())

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        db.blockDao().deleteByNoteId(note.id)
        db.noteDao().deleteById(note.id)
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        db.blockDao().deleteByNoteId(id)
        db.noteDao().deleteById(id)
    }

    // Blocks
    fun getBlocksForNote(noteId: Long): Flow<List<Block>> =
        db.blockDao().getByNoteId(noteId).map { list -> list.map { it.toModel() } }

    suspend fun getBlocksForNoteOnce(noteId: Long): List<Block> =
        db.blockDao().getByNoteIdOnce(noteId).map { it.toModel() }

    suspend fun saveBlocksForNote(noteId: Long, blocks: List<Block>) = withContext(Dispatchers.IO) {
        db.blockDao().deleteByNoteId(noteId)
        val entities = blocks.mapIndexed { index, block ->
            block.copy(noteId = noteId, order = index).toEntity()
        }
        db.blockDao().insertAll(entities)
    }

    suspend fun insertBlock(block: Block): Long =
        db.blockDao().insert(block.toEntity())

    suspend fun updateBlock(block: Block) =
        db.blockDao().update(block.toEntity())

    suspend fun deleteBlock(block: Block) =
        db.blockDao().delete(block.toEntity())

    // Combined
    suspend fun getNoteWithBlocks(noteId: Long): NoteWithBlocks? = withContext(Dispatchers.IO) {
        val note = db.noteDao().getById(noteId) ?: return@withContext null
        val blocks = db.blockDao().getByNoteIdOnce(noteId)
        NoteWithBlocks(note, blocks)
    }

    suspend fun saveNoteWithBlocks(note: Note, blocks: List<Block>) = withContext(Dispatchers.IO) {
        val noteId = if (note.id == 0L) {
            db.noteDao().insert(note.toEntity())
        } else {
            db.noteDao().update(note.toEntity())
            note.id
        }
        db.blockDao().deleteByNoteId(noteId)
        val entities = blocks.mapIndexed { index, block ->
            block.copy(noteId = noteId, order = index).toEntity()
        }
        db.blockDao().insertAll(entities)
        noteId
    }

    // Mappers
    private fun CategoryEntity.toModel() = Category(id = id, name = name, color = color)
    private fun Category.toEntity() = CategoryEntity(id = id, name = name, color = color)

    private fun NoteEntity.toModel() = Note(id = id, title = title, categoryId = categoryId, createdAt = createdAt, updatedAt = updatedAt)
    private fun Note.toEntity() = NoteEntity(id = id, title = title, categoryId = categoryId, createdAt = createdAt, updatedAt = updatedAt)

    private fun BlockEntity.toModel() = Block(
        id = id,
        noteId = noteId,
        type = BlockType.valueOf(type.uppercase()),
        content = content,
        order = orderIndex,
        isChecked = isChecked
    )
    private fun Block.toEntity() = BlockEntity(
        id = id,
        noteId = noteId,
        type = type.name.lowercase(),
        content = content,
        orderIndex = order,
        isChecked = isChecked
    )
}
