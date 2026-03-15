package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.database.entity.IndexedDocuments
import com.vad1mchk.litsearchbot.database.entity.RegisterRequests
import com.vad1mchk.litsearchbot.database.entity.SearchQueryLookups
import com.vad1mchk.litsearchbot.database.entity.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Files

abstract class DatabaseTestBase {
    private lateinit var dbFile: java.nio.file.Path

    @BeforeEach
    fun setUpDatabase() {
        dbFile = Files.createTempFile("litsearchbot-test-", ".db")
        Database.connect(
            url = "jdbc:sqlite:${dbFile.toAbsolutePath()}",
            driver = "org.sqlite.JDBC",
        )

        transaction {
            SchemaUtils.create(Users, RegisterRequests, IndexedDocuments, SearchQueryLookups)
            IndexedDocumentsDao.initFts()
        }
    }

    @AfterEach
    fun tearDownDatabase() {
        Files.deleteIfExists(dbFile)
    }
}
