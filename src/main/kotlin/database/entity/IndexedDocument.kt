package com.vad1mchk.litsearchbot.database.entity

import org.jetbrains.exposed.sql.ResultRow

data class IndexedDocument(
    val hashsum: String,
    val original: String,
    val mode: String,
    val extractedAt: Long,
    val lastModifiedAt: Long,
    val telegramFileId: String?,
) {
    companion object {
        @JvmStatic
        fun fromResultRow(row: ResultRow) = IndexedDocument(
            hashsum = row[IndexedDocuments.hashsum],
            original = row[IndexedDocuments.original],
            mode = row[IndexedDocuments.mode],
            extractedAt = row[IndexedDocuments.extractedAt],
            lastModifiedAt = row[IndexedDocuments.lastModifiedAt],
            telegramFileId = row[IndexedDocuments.telegramFileId],
        )
    }
}
