package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.database.entity.IndexedDocument
import com.vad1mchk.litsearchbot.database.entity.IndexedDocuments
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object IndexedDocumentsDao {

    /**
     * Initializes the FTS5 table and TRIGGERS to keep it in sync automatically.
     * This way, you only ever interact with the main [IndexedDocuments] table.
     */
    fun initFts() = transaction {
        // 1. Create the virtual table
        exec("CREATE VIRTUAL TABLE IF NOT EXISTS indexed_docs_fts USING fts5(hashsum UNINDEXED, content);")

        // 2. Create triggers for automatic Sync (Insert, Update, Delete)
        exec("""
            CREATE TRIGGER IF NOT EXISTS fts_insert AFTER INSERT ON indexed_documents BEGIN
                INSERT INTO indexed_docs_fts(hashsum, content) VALUES (new.hashsum, '');
            END;
        """.trimIndent())

        exec("""
            CREATE TRIGGER IF NOT EXISTS fts_delete AFTER DELETE ON indexed_documents BEGIN
                DELETE FROM indexed_docs_fts WHERE hashsum = old.hashsum;
            END;
        """.trimIndent())
    }

    fun getByHashsum(hashsum: String): IndexedDocument? = transaction {
        IndexedDocuments.selectAll().where { IndexedDocuments.hashsum eq hashsum }
            .singleOrNull()
            ?.let { IndexedDocument.fromResultRow(it) }
    }

    fun getAllMetadataByOriginalPath(): Map<String, Long> = transaction {
        IndexedDocuments.selectAll().associate {
            it[IndexedDocuments.original] to it[IndexedDocuments.lastModifiedAt]
        }
    }

    fun deleteByPath(relativePath: String) = transaction {
        // Trigger fts_delete handles the FTS table automatically
        IndexedDocuments.deleteWhere { original eq relativePath }
    }

    fun updateDocument(hash: String, path: String, ext: String, lastModified: Long, text: String) = transaction {
        IndexedDocuments.upsert {
            it[hashsum] = hash
            it[original] = path
            it[mode] = ext.uppercase()
            it[extractedAt] = System.currentTimeMillis() / 1000
            it[lastModifiedAt] = lastModified
        }

        // Since FTS content usually comes from an external parser,
        // we update the FTS table specifically using parameterized exec to avoid injection.
        exec("UPDATE indexed_docs_fts SET content = ? WHERE hashsum = ?", args = listOf(
            TextColumnType() to text,
            TextColumnType() to hash
        ))
    }

    fun updateFileId(hash: String, fileId: String) = transaction {
        IndexedDocuments.update(where = { IndexedDocuments.hashsum eq hash }) {
            it[telegramFileId] = fileId
        }
    }

    fun search(query: String, limit: Int = 10, offset: Int = 0): List<Pair<String, String>> = transaction {
        val results = mutableListOf<Pair<String, String>>()

        // We use the 'args' parameter to prevent SQL injection
        val sql = """
            SELECT d.hashsum, d.original
            FROM indexed_documents d
            JOIN indexed_docs_fts f ON d.hashsum = f.hashsum
            WHERE f.content MATCH ?
            LIMIT ? OFFSET ?
        """.trimIndent()

        // Correct usage of exec with a result set mapper
        exec(
            sql,
            args = listOf(TextColumnType() to query, IntegerColumnType() to limit, IntegerColumnType() to offset),
        ) { rs ->
            while (rs.next()) {
                results.add(rs.getString("hashsum") to rs.getString("original"))
            }
        }
        results
    }

    fun countSearchResults(query: String): Long = transaction {
        val sql = """
        SELECT COUNT(*)
        FROM indexed_documents d
        JOIN indexed_docs_fts f ON d.hashsum = f.hashsum
        WHERE f.content MATCH ?
    """.trimIndent()

        exec(
            stmt = sql,
            args = listOf(TextColumnType() to query),
        ) { rs ->
            if (rs.next()) rs.getLong(1) else 0L
        } ?: 0L
    }

    fun searchWithSnippet(
        query: String,
        limit: Int = 10,
        offset: Int = 0
    ): List<Triple<String, String, String>> = transaction {
        val results = mutableListOf<Triple<String, String, String>>()

        // Column 1 in indexed_docs_fts is 'content' (0 is hashsum, 1 is content)
        // We use [<b>] and [</b>] as markers for the UI to handle later
        val sql = """
        SELECT d.hashsum, d.original, snippet(indexed_docs_fts, 1, '<b>', '</b>', '...', 24) as excerpt
        FROM indexed_documents d
        JOIN indexed_docs_fts ON d.hashsum = indexed_docs_fts.hashsum
        WHERE indexed_docs_fts.content MATCH ?
        ORDER BY rank
        LIMIT ? OFFSET ?
    """.trimIndent()

        exec(
            sql,
            args = listOf(
                TextColumnType() to query,
                IntegerColumnType() to limit,
                IntegerColumnType() to offset
            ),
        ) { rs ->
            while (rs.next()) {
                results.add(
                    Triple(
                        rs.getString("hashsum"),
                        rs.getString("original"),
                        rs.getString("excerpt")
                    )
                )
            }
        }
        results
    }
}