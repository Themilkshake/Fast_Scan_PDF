package com.superfastscan.data.repository

import android.content.Context
import com.superfastscan.data.local.db.DocumentDao
import com.superfastscan.data.local.db.PageDao
import com.superfastscan.data.mapper.toDomain
import com.superfastscan.data.mapper.toEntity
import com.superfastscan.domain.model.ScanDocument
import com.superfastscan.domain.model.ScanPage
import com.superfastscan.domain.repository.DocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    @ApplicationContext private val context: Context
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<ScanDocument>> =
        documentDao.getAllCompleteDocuments().map { entities ->
            entities.map { entity ->
                val pageCount = documentDao.getPageCount(entity.id)
                val pages = pageDao.getPagesByDocumentOnce(entity.id)
                val thumbnail = pages.firstOrNull()?.let { it.editedImagePath ?: it.originalImagePath }
                entity.toDomain(pageCount = pageCount, thumbnailPath = thumbnail)
            }
        }

    override fun getDocumentById(id: Long): Flow<ScanDocument?> =
        documentDao.getDocumentById(id).map { entity ->
            entity?.let {
                val pageCount = documentDao.getPageCount(it.id)
                it.toDomain(pageCount = pageCount)
            }
        }

    override fun getPagesForDocument(documentId: Long): Flow<List<ScanPage>> =
        pageDao.getPagesByDocument(documentId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getPagesList(documentId: Long): List<ScanPage> =
        pageDao.getPagesByDocumentOnce(documentId).map { it.toDomain() }

    override suspend fun createDraftDocument(title: String): Long {
        val entity = com.superfastscan.data.local.entity.DocumentEntity(
            title = title.ifBlank { "Scan ${System.currentTimeMillis()}" },
            isDraft = true
        )
        return documentDao.insert(entity)
    }

    override suspend fun updateDocument(document: ScanDocument) {
        val existing = documentDao.getDocumentByIdOnce(document.id)
        if (existing != null) {
            documentDao.update(
                existing.copy(
                    title = document.title.ifBlank { existing.title },
                    isDraft = document.isDraft,
                    updatedAt = System.currentTimeMillis(),
                    exportedFilePath = document.exportedFilePath ?: existing.exportedFilePath
                )
            )
        }
    }

    override suspend fun markDocumentReady(documentId: Long) {
        documentDao.markReady(documentId)
    }

    override suspend fun deleteDocument(documentId: Long, deleteExportedFiles: Boolean) {
        val document = documentDao.getDocumentByIdOnce(documentId)
        if (deleteExportedFiles && document != null) {
            document.exportedFilePath?.let { path ->
                try {
                    if (path.startsWith("content://")) {
                        val uri = android.net.Uri.parse(path)
                        context.contentResolver.delete(uri, null, null)
                    } else {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Delete images from disk
        val scanDir = File(context.filesDir, "scans/$documentId")
        if (scanDir.exists()) {
            scanDir.deleteRecursively()
        }
        // Room CASCADE will delete pages automatically
        documentDao.deleteById(documentId)
    }

    override suspend fun insertPage(page: ScanPage): Long =
        pageDao.insert(page.toEntity())

    override suspend fun updatePage(page: ScanPage) =
        pageDao.update(page.toEntity())

    override suspend fun deletePage(pageId: Long) {
        val page = pageDao.getPageById(pageId) ?: return
        // Delete image files
        File(page.originalImagePath).delete()
        page.editedImagePath?.let { File(it).delete() }
        pageDao.deleteById(pageId)
    }

    override suspend fun reorderPages(documentId: Long, pages: List<ScanPage>) {
        pages.forEachIndexed { index, page ->
            pageDao.updatePageIndex(page.id, index)
        }
    }

    override suspend fun getActiveDraft(): ScanDocument? =
        documentDao.getActiveDraft()?.toDomain()
}
