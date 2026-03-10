package com.vad1mchk.litsearchbot.documents

import com.vad1mchk.litsearchbot.database.IndexedDocumentsDao
import org.apache.commons.codec.digest.DigestUtils
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.extractor.ExtractorFactory
import java.io.File

class IndexingService(private val literaturePath: String) {
    private var stats = IndexingStats()

    fun getStats() = stats

    fun reindex(onProgress: (Int) -> Unit) {
        val root = File(literaturePath)
        val supported = setOf("txt", "pdf", "docx")

        val diskFiles = root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in supported }
            .associateBy { it.relativeTo(root).path }

        val dbFiles = IndexedDocumentsDao.getAllMetadata()
        var currentStats = IndexingStats()

        // 1. Handle Deletions (In DB but not on disk)
        dbFiles.keys.filter { it !in diskFiles }.forEach { path ->
            IndexedDocumentsDao.deleteByPath(path)
            currentStats = currentStats.copy(deleted = currentStats.deleted + 1)
        }

        // 2. Handle Additions and Updates
        val total = diskFiles.size
        diskFiles.entries.forEachIndexed { index, (path, file) ->
            val lastModified = file.lastModified() / 1000
            val dbLastModified = dbFiles[path]

            when {
                dbLastModified == null -> {
                    // New file
                    processFile(file, path, lastModified)
                    currentStats = currentStats.copy(added = currentStats.added + 1)
                }
                lastModified > dbLastModified -> {
                    // Outdated file
                    processFile(file, path, lastModified)
                    currentStats = currentStats.copy(updated = currentStats.updated + 1)
                }
                else -> {
                    currentStats = currentStats.copy(upToDate = currentStats.upToDate + 1)
                }
            }
            onProgress(((index + 1) * 100) / total)
        }
        stats = currentStats
    }

    private fun processFile(file: File, path: String, lastModified: Long) {
        val text = extractText(file) ?: ""
        val hash = DigestUtils.md5Hex(path)
//        IndexedDocumentsDao.updateDocument(hash, path, file.extension, lastModified, text)
    }

    fun extractText(file: File): String? {
        return try {
            when (file.extension.lowercase()) {
                "txt" -> file.readText(Charsets.UTF_8)
                "pdf" -> {
                    Loader.loadPDF(file).use { document ->
                        PDFTextStripper().getText(document)
                    }
                }
                "docx" -> {
                    file.inputStream().use { fis ->
                        ExtractorFactory.createExtractor(fis).text
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
