package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.auth.RegisterStatus
import com.vad1mchk.litsearchbot.database.entity.RegisterRequest
import com.vad1mchk.litsearchbot.database.entity.RegisterRequests
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object RegisterRequestDao {
    fun create(userId: Long, handle: String?, name: String): Int = transaction {
        RegisterRequests.insert {
            it[RegisterRequests.userId] = userId
            it[lastKnownHandle] = handle
            it[lastKnownName] = name
            it[status] = RegisterStatus.PENDING
            it[createdAt] = System.currentTimeMillis()
        } get RegisterRequests.id
    }

    fun updateHandleAndName(userId: Long, handle: String?, name: String) = transaction {
        RegisterRequests.update({ RegisterRequests.userId eq userId }) {
            it[lastKnownHandle] = handle
            it[lastKnownName] = name
        }
    }

    fun count(status: RegisterStatus?): Long = transaction {
        if (status == null) {
            RegisterRequests.selectAll().count()
        } else {
            RegisterRequests.selectAll().where { RegisterRequests.status eq status }.count()
        }
    }

    fun getById(id: Int): RegisterRequest? = transaction {
        val row = RegisterRequests.selectAll().where { RegisterRequests.id eq id }.lastOrNull()
        row?.let { RegisterRequest.fromResultRow(it) }
    }

    fun lastOrNull(userId: Long): RegisterRequest? = transaction {
        val row = RegisterRequests.selectAll().where { RegisterRequests.userId eq userId }.lastOrNull()
        row?.let { RegisterRequest.fromResultRow(it) }
    }

    fun list(offset: Int, limit: Int, status: RegisterStatus?): List<RegisterRequest> = transaction {
        val q = if (status == null) {
            RegisterRequests.selectAll()
        } else {
            RegisterRequests.selectAll().where { RegisterRequests.status eq status }
        }

        q.limit(limit).offset(offset.toLong())
            .map { RegisterRequest.fromResultRow(it) }
    }

    fun list(status: RegisterStatus? = null) = transaction {
        if (status == null) {
            RegisterRequests.selectAll().toList()
        } else {
            RegisterRequests.selectAll().where { RegisterRequests.status eq status }.toList()
        }
    }

    fun approve(id: Int, adminId: Long) = transaction {
        RegisterRequests.update(
            { (RegisterRequests.id eq id) and (RegisterRequests.status eq RegisterStatus.PENDING) },
        ) {
            it[status] = RegisterStatus.APPROVED
            it[handledAt] = System.currentTimeMillis()
            it[handledBy] = adminId
        }
    }

    fun reject(id: Int, adminId: Long, reason: String? = null) = transaction {
        RegisterRequests.update(
            { (RegisterRequests.id eq id) and (RegisterRequests.status eq RegisterStatus.PENDING) },
        ) {
            it[status] = RegisterStatus.REJECTED
            it[handledAt] = System.currentTimeMillis()
            it[handledBy] = adminId
            it[RegisterRequests.reason] = reason
            // Append reason if you decide to store it
        }
    }

    fun findPending(userId: Long) = transaction {
        RegisterRequests.selectAll().where { RegisterRequests.status eq RegisterStatus.PENDING }.firstOrNull()
    }
}
