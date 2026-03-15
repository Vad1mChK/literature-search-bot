package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.auth.RegisterStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RegisterRequestDaoTest : DatabaseTestBase() {
    @Test
    fun createAndFetchRequest_shouldFetchCorrectData() {
        val id = RegisterRequestDao.create(
            userId = 234L,
            handle = "alice",
            name = "Alice",
        )

        val req = RegisterRequestDao.getById(id)

        assertNotNull(req)
        assertEquals(234L, req.userId)
        assertEquals(RegisterStatus.PENDING, req.status)
    }

    @Test
    fun approveRequest_shouldChangeStatus() {
        val id = RegisterRequestDao.create(
            userId = 267L,
            handle = "bob",
            name = "Bob",
        )

        RegisterRequestDao.approve(id, adminId = 999L)
        val req = RegisterRequestDao.getById(id)

        assertNotNull(req)
        assertEquals(RegisterStatus.APPROVED, req.status)
        assertEquals(999L, req.handledBy)
    }

    @Test
    fun rejectRequest_shouldChangeStatus() {
        val id = RegisterRequestDao.create(
            userId = 501L,
            handle = "charlie_",
            name = "Charlie",
        )

        RegisterRequestDao.reject(id, adminId = 999L, reason = "some reason")
        val req = RegisterRequestDao.getById(id)

        assertNotNull(req)
        assertEquals(RegisterStatus.REJECTED, req.status)
        assertEquals(999L, req.handledBy)
        assertEquals("some reason", req.reason)
    }
}
