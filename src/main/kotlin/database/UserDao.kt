package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.database.entity.User
import com.vad1mchk.litsearchbot.database.entity.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object UserDao {
    fun getById(userId: Long): User? = transaction {
        val row = Users.selectAll()
            .where { Users.userId eq userId }
            .singleOrNull()
        row?.let {
            User(
                userId = row[Users.userId],
                lastKnownHandle = row[Users.lastKnownHandle],
                role = row[Users.role],
                preferences = row[Users.preferences],
            )
        }
    }

    fun getRole(userId: Long): UserRole? = transaction {
        Users.selectAll()
            .where { Users.userId eq userId }
            .singleOrNull()
            ?.get(Users.role)
    } // In many use cases, user is a guest when not inserted in the table

    fun existsUser(userId: Long) = transaction {
        Users.selectAll().where { Users.userId eq userId }.any()
    }

    fun upsertUser(
        userId: Long,
        userRole: UserRole,
        handle: String? = null,
    ) = transaction {
        val exists = Users.selectAll().where { Users.userId eq userId }.any()

        if (exists) {
            Users.update({ Users.userId eq userId }) {
                it[Users.role] = userRole
                if (handle != null) it[Users.lastKnownHandle] = handle
            }
        } else {
            Users.insert {
                it[Users.userId] = userId
                it[Users.role] = userRole
                it[Users.lastKnownHandle] = handle
            }
        }
    }

    fun updateHandle(userId: Long, handle: String? = null) = transaction {
        val exists = Users.selectAll().where { Users.userId eq userId }.any()

        if (exists) {
            Users.update({ Users.userId eq userId }) {
                it[Users.lastKnownHandle] = handle
            }
        }
    }

    fun deleteUser(userId: Long) = transaction {
        Users.deleteWhere { Users.userId eq userId }
    }

    fun listUsers(userRole: UserRole? = null): List<User> = transaction {
        val query = if (userRole == null) {
            Users.selectAll()
        } else {
            Users.selectAll().where { Users.role eq userRole }
        }

        query.map { row ->
            User(
                userId = row[Users.userId],
                lastKnownHandle = row[Users.lastKnownHandle],
                role = row[Users.role],
                preferences = row[Users.preferences],
            )
        }
    }

    fun listUsers(limit: Int, offset: Int): List<User> = transaction {
        Users
            .selectAll()
            .limit(count = limit).offset(offset.toLong())
            .map {
                User(
                    userId = it[Users.userId],
                    lastKnownHandle = it[Users.lastKnownHandle],
                    role = it[Users.role],
                    preferences = it[Users.preferences],
                )
            }
    }

    fun countUsers(): Long = transaction {
        Users.selectAll().count()
    }
}
