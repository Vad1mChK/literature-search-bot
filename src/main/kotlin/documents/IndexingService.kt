package com.vad1mchk.litsearchbot.documents

import com.vad1mchk.litsearchbot.database.IndexedDocumentsDao
import org.apache.commons.codec.digest.DigestUtils
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.extractor.ExtractorFactory
import java.io.File

/**
 * Service responsible for indexing literature (monitoring the state of the files and extracting their contents).
 * @property literaturePath Path to the literature directory.
 */
class IndexingService(private val literaturePath: String) {
    /**
     * Reindexes the literature directory, updating the index based on the current state of the files.
     * @param onProgress Optional callback on progress, taking in the count of files already reindexed and the total
     * count. May be useful for UI updates.
     * @return Stats object describing changes as a result of this reindex run.
     */
    fun reindex(onProgress: ((current: Int, total: Int) -> Unit)? = null): IndexingDeltaStats {
        val root = File(literaturePath)
        val supported = setOf("txt", "pdf", "docx")

        val runStart = System.currentTimeMillis()
        var stats = IndexingDeltaStats(runStartedAt = runStart, runFinishedAt = runStart)

        val diskFiles = root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in supported }
            .associateBy { it.relativeTo(root).path }
        val dbFiles = IndexedDocumentsDao.getAllMetadataByOriginalPath()

        println("db files: $dbFiles")
        println("disk files: $diskFiles")

        // 1. Handle Deletions (In DB but not on disk)
        dbFiles.keys.filter { it !in diskFiles }.forEach { path ->
            IndexedDocumentsDao.deleteByPath(path)
            stats = stats.copy(deleted = stats.deleted + 1)
        }

        // 2. Handle Additions and Updates
        val total = diskFiles.size
        diskFiles.entries.forEachIndexed { index, (path, file) ->
            val lastModified = file.lastModified() / 1000
            val dbLastModified = dbFiles[path]

            try {
                when {
                    dbLastModified == null -> {
                        // New file
                        processFile(file, path, lastModified)
                        stats = stats.copy(added = stats.added + 1)
                    }

                    lastModified > dbLastModified -> {
                        // Outdated file
                        processFile(file, path, lastModified)
                        stats = stats.copy(updated = stats.updated + 1)
                    }

                    else -> {
                        stats = stats.copy(upToDate = stats.upToDate + 1)
                    }
                }
            } catch (e: Exception) {
                stats = stats.copy(failed = stats.failed + 1)
            }
            onProgress?.invoke(index + 1, total)
        }

        stats = stats.copy(runFinishedAt = System.currentTimeMillis())
        return stats
    }

    /**
     * Computes total stats of the current index without reindexing.
     * @return Stats object describing the current index state (files up-to-date, missing, unindexed etc.)
     */
    fun computeTotalStats(): IndexingTotalStats {
        val root = File(literaturePath)
        val supported = setOf("txt", "pdf", "docx")

        val diskFiles: Map<String, Long> = root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in supported }
            .associate { file ->
                val rel = file.relativeTo(root).path
                rel to file.lastModified() / 1000
            }

        val dbFiles: Map<String, Long> = IndexedDocumentsDao.getAllMetadataByOriginalPath() // path -> lastModifiedAt

        val diskPaths = diskFiles.keys
        val dbPaths = dbFiles.keys

        val totalOnDisk = diskPaths.size
        val totalInDb = dbPaths.size

        val disappeared = (dbPaths - diskPaths).size // in DB, not on disk
        val unindexed = (diskPaths - dbPaths).size // on disk, not in DB

        var upToDate = 0
        var outdated = 0

        for (path in diskPaths.intersect(dbPaths)) {
            val diskTs = diskFiles[path]!!
            val dbTs = dbFiles[path]!!
            if (dbTs >= diskTs) upToDate++ else outdated++
        }

        val newestDiskTimestamp = diskFiles.values.maxOrNull() ?: 0L
        val newestIndexedTimestamp = dbFiles.values.maxOrNull() ?: 0L

        return IndexingTotalStats(
            totalOnDisk = totalOnDisk,
            totalInDatabase = totalInDb,
            upToDate = upToDate,
            outdated = outdated,
            disappeared = disappeared,
            unindexed = unindexed,
            newestDiskTimestamp = newestDiskTimestamp,
            newestIndexedTimestamp = newestIndexedTimestamp,
        )
    }

    private fun processFile(file: File, path: String, lastModified: Long) {
        val text = extractText(file) ?: ""
        val hash = DigestUtils.md5Hex(path)
        IndexedDocumentsDao.updateDocument(hash, path, file.extension, lastModified, text)
    }

    /**
     * Extracts text from this file, using dedicated extraction methods.
     * @return The text contents on the file on success, or `null` on failure.
     */
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
