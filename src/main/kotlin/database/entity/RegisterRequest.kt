package com.vad1mchk.litsearchbot.database.entity

import com.vad1mchk.litsearchbot.auth.RegisterStatus
import org.jetbrains.exposed.sql.ResultRow

data class RegisterRequest(
    val id: Int,
    val userId: Long,
    val handle: String?,
    val name: String?,
    val status: RegisterStatus,
    val createdAt: Long,
    val handledAt: Long?,
    val handledBy: Long?,
    val reason: String? = null,
) {
    companion object {
        @JvmStatic
        fun fromResultRow(row: ResultRow) = RegisterRequest(
            id = row[RegisterRequests.id],
            userId = row[RegisterRequests.userId],
            handle = row[RegisterRequests.lastKnownHandle],
            name = row[RegisterRequests.lastKnownName],
            status = row[RegisterRequests.status],
            createdAt = row[RegisterRequests.createdAt],
            handledAt = row[RegisterRequests.handledAt],
            handledBy = row[RegisterRequests.handledBy],
            reason = row[RegisterRequests.reason],
        )
    }
}
