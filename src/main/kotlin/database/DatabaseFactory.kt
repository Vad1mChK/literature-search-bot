package com.vad1mchk.litsearchbot.database

import com.vad1mchk.litsearchbot.database.entity.IndexedDocuments
import com.vad1mchk.litsearchbot.database.entity.RegisterRequests
import com.vad1mchk.litsearchbot.database.entity.SearchQueryLookups
import com.vad1mchk.litsearchbot.database.entity.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    val logger = LoggerFactory.getLogger("DatabaseFactory")

    fun init(dbPath: String) {
        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")

        transaction {
            // Create tables if they don't exist
            SchemaUtils.create(Users, RegisterRequests, IndexedDocuments, SearchQueryLookups)
            IndexedDocumentsDao.initFts()
            //
        }

        logger.info("Database tables created: Users, RegisterRequests, IndexedDocuments, SearchQueryLookups")
    }
}
