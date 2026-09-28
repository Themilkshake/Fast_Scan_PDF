package com.superfastscan.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.superfastscan.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE isDraft = 0 ORDER BY updatedAt DESC")
    fun getAllCompleteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentByIdOnce(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE isDraft = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveDraft(): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Query("UPDATE documents SET isDraft = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markReady(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM pages WHERE documentId = :documentId")
    suspend fun getPageCount(documentId: Long): Int
}
