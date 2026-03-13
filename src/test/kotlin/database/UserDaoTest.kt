package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.auth.UserRole
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserDaoTest : DatabaseTestBase() {
    @Test
    fun upsertUser_withRole_shouldPreserveRole() {
        UserDao.upsertUser(userId = 234L, userRole = UserRole.ADMIN)
        UserDao.upsertUser(userId = 267L, userRole = UserRole.REGULAR)

        assertEquals(UserRole.ADMIN, UserDao.getRole(234L))
        assertEquals(UserRole.REGULAR, UserDao.getRole(267L))
    }

    @Test
    fun countUsers_withUsersInserted_shouldCorrectlyCount() {
        val count = 20L
        for (i in 0L..<count) {
            UserDao.upsertUser(userId = i, userRole = UserRole.REGULAR)
        }
        assertEquals(count, UserDao.countUsers())
        assertEquals(count, UserDao.listUsers().size.toLong())
    }

    @Test
    fun deleteUser_withUserPresent_shouldCorrectlyDeleteUser() {
        val userId = 234L
        UserDao.upsertUser(userId, userRole = UserRole.ADMIN)
        assertNotNull(UserDao.getById(userId))
        assertTrue(UserDao.existsUser(userId))

        val deletedCount = UserDao.deleteUser(userId)
        assertEquals(1, deletedCount)
        assertNull(UserDao.getById(userId))
        assertFalse(UserDao.existsUser(userId))
    }

    @Test
    fun updateHandle_withUserPresent_shouldUpdateHandleCorrectly() {
        val userId = 267L
        UserDao.upsertUser(userId, userRole = UserRole.REGULAR, handle = "eddie")
        assertEquals("eddie", UserDao.getById(userId)?.lastKnownHandle)

        UserDao.updateHandle(userId, handle = "venom")
        assertEquals("venom", UserDao.getById(userId)?.lastKnownHandle)
    }
}
