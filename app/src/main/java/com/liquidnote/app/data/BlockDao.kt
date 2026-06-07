package com.liquidnote.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {
    @Query("SELECT * FROM blocks WHERE noteId = :noteId ORDER BY orderIndex ASC")
    fun getByNoteId(noteId: Long): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks WHERE noteId = :noteId ORDER BY orderIndex ASC")
    suspend fun getByNoteIdOnce(noteId: Long): List<BlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: BlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<BlockEntity>)

    @Update
    suspend fun update(block: BlockEntity)

    @Delete
    suspend fun delete(block: BlockEntity)

    @Query("DELETE FROM blocks WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    @Query("DELETE FROM blocks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
