package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.database.entity.IndexedDocuments
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object IndexedDocumentsDao {
    // Since Exposed doesn't have a native FTS5 DSL, we use raw SQL for the virtual table
    fun initFts() = transaction {
        exec("CREATE VIRTUAL TABLE IF NOT EXISTS indexed_docs_fts USING fts5(hashsum UNINDEXED, content);")
    }

    fun getAllMetadata(): Map<String, Long> = transaction {
        IndexedDocuments.selectAll().associate { it[IndexedDocuments.original] to it[IndexedDocuments.lastModifiedAt] }
    }

    fun deleteByPath(relativePath: String) = transaction {
        val hash = IndexedDocuments.select(IndexedDocuments.hashsum)
            .where { IndexedDocuments.original eq relativePath }
            .singleOrNull()?.get(IndexedDocuments.hashsum)

        if (hash != null) {
            IndexedDocuments.deleteWhere { original eq relativePath }
            exec("DELETE FROM indexed_docs_fts WHERE hashsum = '$hash'")
        }
    }

//    fun updateDocument(hash: String, path: String, ext: String, lastModified: Long, text: String) = transaction {
//        IndexedDocuments.replace {
//            it[hashsum] = hash
//            it[original] = path
//            it[mode] = ext.uppercase()
//            it[extractedAt] = System.currentTimeMillis() / 1000
//            it[lastModifiedAt] = lastModified
//        }
//        // Update FTS: Delete old entry and insert new one
//        exec("DELETE FROM indexed_docs_fts WHERE hashsum = '$hash'")
//        exec("INSERT INTO indexed_docs_fts (hashsum, content) VALUES (?, ?)") {
//            it.setString(1, hash)
//            it.setString(2, text)
//        }
//    }

//    fun search(query: String): List<Pair<String, String>> = transaction {
//        val results = mutableListOf<Pair<String, String>>()
//        // FTS5 MATCH query for high performance
//        val sql = """
//            SELECT d.hashsum, d.original
//            FROM indexed_documents d
//            JOIN indexed_docs_fts f ON d.hashsum = f.hashsum
//            WHERE f.content MATCH ?
//            LIMIT 10
//        """.trimIndent()
//
//        exec(sql, listOf(query)) { rs ->
//            while (rs.next()) {
//                results.add(rs.getString("hashsum") to rs.getString("original"))
//            }
//        }
//        results
//    }
}
