package com.superfastscan.domain.repository

import com.superfastscan.domain.model.ScanDocument
import com.superfastscan.domain.model.ScanPage
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<ScanDocument>>
    fun getDocumentById(id: Long): Flow<ScanDocument?>
    fun getPagesForDocument(documentId: Long): Flow<List<ScanPage>>
    suspend fun getPagesList(documentId: Long): List<ScanPage>
    suspend fun createDraftDocument(title: String = ""): Long
    suspend fun updateDocument(document: ScanDocument)
    suspend fun markDocumentReady(documentId: Long)
    suspend fun deleteDocument(documentId: Long, deleteExportedFiles: Boolean = false)
    suspend fun insertPage(page: ScanPage): Long
    suspend fun updatePage(page: ScanPage)
    suspend fun deletePage(pageId: Long)
    suspend fun reorderPages(documentId: Long, pages: List<ScanPage>)
    suspend fun getActiveDraft(): ScanDocument?
}
