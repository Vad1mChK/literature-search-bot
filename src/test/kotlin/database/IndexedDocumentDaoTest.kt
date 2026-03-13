package com.vad1mchk.litsearchbot.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IndexedDocumentDaoTest : DatabaseTestBase() {
    @Test
    fun updateDocument_newDocument_createsRecordAndFtsEntry() {
        val hash = "abc123hash"
        val path = "path/to/book.pdf"
        val content = "This is the content containing the word sophon."

        IndexedDocumentsDao.updateDocument(hash, path, "pdf", 1000L, content)

        val retrieved = IndexedDocumentsDao.getByHashsum(hash)
        assertNotNull(retrieved)

        val searchResult = IndexedDocumentsDao.search("sophon")
        assertEquals(1, searchResult.size)
        assertEquals(hash, searchResult[0].first)
    }

    @Test
    fun searchWithSnippet_existingMatch_returnsHighlightedExcerpt() {
        val hash = "hash_snippet"
        val content = "Professor Wang Miao is the one who uncovered the mystery."

        IndexedDocumentsDao.updateDocument(hash, "wang.pdf", "pdf", 123L, content)

        val results = IndexedDocumentsDao.searchWithSnippet("Wang Miao")

        assertEquals(1, results.size)
        val (_, _, excerpt) = results[0]
        assertTrue(excerpt.contains("<b>Wang</b>"))
        assertTrue(excerpt.contains("<b>Miao</b>"))
    }

    @Test
    fun getAllMetadataByOriginalPath_multipleDocuments_returnsCorrectMapping() {
        IndexedDocumentsDao.updateDocument("h1", "path1", "txt", 100L, "text1")
        IndexedDocumentsDao.updateDocument("h2", "path2", "txt", 200L, "text2")

        val metadata = IndexedDocumentsDao.getAllMetadataByOriginalPath()

        assertEquals(2, metadata.size)
        assertEquals(100L, metadata["path1"])
        assertEquals(200L, metadata["path2"])
    }

    @Test
    fun countSearchResults_multipleMatches_returnsTotalCount() {
        IndexedDocumentsDao.updateDocument("h1", "p1", "txt", 0L, "find me")
        IndexedDocumentsDao.updateDocument("h2", "p2", "txt", 0L, "find me too")
        IndexedDocumentsDao.updateDocument("h3", "p3", "txt", 0L, "ignore")

        val count = IndexedDocumentsDao.countSearchResults("find")
        assertEquals(2, count)
    }

    @Test
    fun updateDocument_existingHash_updatesMetadataInsteadOfCreatingDuplicate() {
        val hash = "stable_hash"
        IndexedDocumentsDao.updateDocument(hash, "old_path.pdf", "pdf", 100L, "content")
        IndexedDocumentsDao.updateDocument(hash, "new_path.pdf", "pdf", 200L, "content")

        val doc = IndexedDocumentsDao.getByHashsum(hash)
        assertEquals("new_path.pdf", doc?.original)
        assertEquals(200L, doc?.lastModifiedAt)
    }

    @Test
    fun deleteByPath_existingDocument_removesFromMainTableAndFts() {
        val hash = "to_delete"
        val path = "delete_me.pdf"
        IndexedDocumentsDao.updateDocument(hash, path, "pdf", 0L, "sophon")

        IndexedDocumentsDao.deleteByPath(path)

        assertNull(IndexedDocumentsDao.getByHashsum(hash))
        assertTrue(IndexedDocumentsDao.search("sophon").isEmpty())
    }

    @Test
    fun updateFileId_existingDocument_updatesTelegramFileIdField() {
        val hash = "file_id_test"
        IndexedDocumentsDao.updateDocument(hash, "file.pdf", "pdf", 0L, "content")
        val fileId = "AgACAgIAAx..."

        IndexedDocumentsDao.updateFileId(hash, fileId)

        val doc = IndexedDocumentsDao.getByHashsum(hash)
        assertEquals(fileId, doc?.telegramFileId)
    }
}
