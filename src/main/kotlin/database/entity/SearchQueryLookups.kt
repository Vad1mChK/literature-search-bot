package com.vad1mchk.litsearchbot.database.entity

import org.jetbrains.exposed.sql.Table

object SearchQueryLookups : Table("search_query_lookup") {
    val queryHash = varchar("query_hash", 64)
    val queryText = text("query_text")
    val lastUsedAt = long("last_used_at")

    override val primaryKey = PrimaryKey(queryHash)
}