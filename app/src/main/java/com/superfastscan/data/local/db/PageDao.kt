package com.superfastscan.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.superfastscan.data.local.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getPagesByDocument(documentId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getPagesByDocumentOnce(documentId: Long): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getPageById(id: Long): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity): Long

    @Update
    suspend fun update(page: PageEntity)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deleteAllForDocument(documentId: Long)

    @Query("UPDATE pages SET pageIndex = :newIndex WHERE id = :pageId")
    suspend fun updatePageIndex(pageId: Long, newIndex: Int)
}
