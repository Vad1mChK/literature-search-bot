package com.vad1mchk.litsearchbot.database.entity

import com.vad1mchk.litsearchbot.auth.UserRole
import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val userId = long("user_id") // Telegram ID
    val lastKnownHandle = varchar("last_known_handle", 64).nullable()
    val role = enumerationByName("role", 16, UserRole::class) // "REGULAR", "ADMIN"
    val preferences = text("preferences").default("{}")

    override val primaryKey = PrimaryKey(userId)
}
