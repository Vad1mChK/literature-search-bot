package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.util.md5
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SearchQueryLookupDaoTest : DatabaseTestBase() {
    @BeforeEach
    fun decreaseCapacity() {
        SearchQueryLookupDao.maxCapacity = 10
    }

    @Test
    fun findByHash_ifQueryPresent_shouldCorrectlyFindQuery() {
        val query1 = "Wang Miao"
        val query2 = "Shi Qiang"
        val hash1 = md5(query1)
        val hash2 = md5(query2)

        SearchQueryLookupDao.upsertQuery(hash1, query1, 234L)
        SearchQueryLookupDao.upsertQuery(hash2, query2, 267L)

        assertEquals(query1, SearchQueryLookupDao.findByHash(hash1))
        assertEquals(query2, SearchQueryLookupDao.findByHash(hash2))
    }

    @Test
    fun shouldPreserveNoMoreThanMaxCapacity() {
        val capacity = SearchQueryLookupDao.maxCapacity
        val count = (capacity * 2).toLong()

        for (i in 0L..<count) {
            val text = "query$i"
            val hash = "hash$i"
            SearchQueryLookupDao.upsertQuery(hash, text, i)
        }

        assertNotNull(SearchQueryLookupDao.findByHash("hash${count - 1}"))
        assertNotNull(SearchQueryLookupDao.findByHash("hash${count - capacity}"))
        assertNull(SearchQueryLookupDao.findByHash("hash${count - capacity - 1}"))
        assertNull(SearchQueryLookupDao.findByHash("hash0"))
    }

    @AfterEach
    fun restoreCapacity() {
        SearchQueryLookupDao.maxCapacity = SearchQueryLookupDao.DEFAULT_MAX_CAPACITY
    }
}
