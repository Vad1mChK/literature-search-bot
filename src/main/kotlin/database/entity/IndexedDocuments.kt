package com.vad1mchk.litsearchbot.database.entity

import org.jetbrains.exposed.sql.Table

object IndexedDocuments : Table("indexed_documents") {
    val hashsum = varchar("hashsum", 32)
    val original = varchar("original", 512)
    val mode = varchar("mode", 20)
    val extractedAt = long("extracted_at")
    val lastModifiedAt = long("last_modified_at")
//    val extractedText = text("extracted_text")

    override val primaryKey = PrimaryKey(hashsum)
}
