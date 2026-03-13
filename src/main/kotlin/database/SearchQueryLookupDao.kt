package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.database.entity.SearchQueryLookups
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object SearchQueryLookupDao {
    private const val MAX_CAPACITY = 1024

    /**
     * Finds a query by its hash and updates the [lastUsedAt] timestamp.
     */
    fun findByHash(hash: String): String? = transaction {
        val row = SearchQueryLookups
            .select(SearchQueryLookups.queryText)
            .where { SearchQueryLookups.queryHash eq hash }
            .singleOrNull()

        if (row != null) {
            val text = row[SearchQueryLookups.queryText]
            updateTimestamp(hash)
            text
        } else {
            null
        }
    }

    /**
     * Inserts or updates a query, ensuring the table does not exceed [MAX_CAPACITY].
     */
    fun upsertQuery(hash: String, text: String) = transaction {
        val now = Instant.now().toEpochMilli()

        val updatedCount = SearchQueryLookups.update({ SearchQueryLookups.queryHash eq hash }) {
            it[queryText] = text
            it[lastUsedAt] = now
        }

        if (updatedCount == 0) {
            // New entry: Check capacity before inserting
            ensureCapacity(requiredSpace = 1)
            SearchQueryLookups.insert {
                it[queryHash] = hash
                it[queryText] = text
                it[lastUsedAt] = now
            }
        }
    }

    private fun updateTimestamp(hash: String) {
        SearchQueryLookups.update({ SearchQueryLookups.queryHash eq hash }) {
            it[SearchQueryLookups.lastUsedAt] = Instant.now().toEpochMilli()
        }
    }

    private fun ensureCapacity(requiredSpace: Int) {
        val currentCount = SearchQueryLookups.selectAll().count()
        val overflow = (currentCount + requiredSpace) - MAX_CAPACITY

        if (overflow > 0) {
            // Select the hashes of the N oldest entries to delete
            val hashesToDelete = SearchQueryLookups
                .select(SearchQueryLookups.queryHash)
                .orderBy(SearchQueryLookups.lastUsedAt, SortOrder.ASC)
                .limit(overflow.toInt())
                .map { it[SearchQueryLookups.queryHash] }

            if (hashesToDelete.isNotEmpty()) {
                SearchQueryLookups.deleteWhere {
                    queryHash inList hashesToDelete
                }
            }
        }
    }
}
