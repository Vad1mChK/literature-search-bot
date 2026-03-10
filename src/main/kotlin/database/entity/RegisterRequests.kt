package com.vad1mchk.litsearchbot.database.entity

import com.vad1mchk.litsearchbot.auth.RegisterStatus
import org.jetbrains.exposed.sql.Table

object RegisterRequests : Table("auth_requests") {
    val id = integer("id").autoIncrement()
    val userId = long("user_id")
    val lastKnownHandle = varchar("last_known_handle", 64).nullable()
    val lastKnownName = varchar("last_known_name", 128).nullable()
    val status = enumerationByName("status", 16, RegisterStatus::class) // PENDING, APPROVED, REJECTED
    val createdAt = long("created_at")
    val handledAt = long("handled_at").nullable()
    val handledBy = long("handled_by").nullable()
    val reason = text("reason").nullable()

    override val primaryKey = PrimaryKey(id)
}
