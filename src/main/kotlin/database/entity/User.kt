package com.vad1mchk.litsearchbot.database.entity

import com.vad1mchk.litsearchbot.auth.UserRole

data class User(
    val userId: Long,
    val lastKnownHandle: String?,
    val role: UserRole,
    val preferences: String = "{}",
)
