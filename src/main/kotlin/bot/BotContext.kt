package com.vad1mchk.litsearchbot.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.logging.LogLevel
import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.commands.CallbackQueries
import com.vad1mchk.litsearchbot.commands.Commands
import com.vad1mchk.litsearchbot.commands.withMinRole
import com.vad1mchk.litsearchbot.database.DatabaseFactory
import com.vad1mchk.litsearchbot.database.UserDao
import com.vad1mchk.litsearchbot.documents.IndexingService
import io.github.cdimascio.dotenv.dotenv
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.text.split

object BotContext {
    // Env vars
    @JvmStatic val LSB_BOT_TOKEN: String

    @JvmStatic val LSB_ADMINS_ON_START: List<Long>

    @JvmStatic val LSB_DB_PATH: String

    @JvmStatic val LSB_LITERATURE_PATH: String

    // App globals
    @JvmStatic lateinit var bot: Bot
    lateinit var indexingService: IndexingService

    init {
        val env = dotenv()
        LSB_BOT_TOKEN = env["LSB_BOT_TOKEN"]
            ?: throw IllegalArgumentException("Env variable LSB_BOT_TOKEN is not set")
        LSB_ADMINS_ON_START = env["LSB_ADMINS_ON_START"]
            ?.split(",")
            ?.mapNotNull { s -> s.toLongOrNull() }
            ?: emptyList()
        LSB_DB_PATH = env["LSB_DB_PATH"]
            ?: throw IllegalArgumentException("Env variable LSB_DB_PATH is not set")
        LSB_LITERATURE_PATH = env["LSB_LITERATURE_PATH"]
            ?: throw IllegalArgumentException("Env variable LSB_LITERATURE_PATH is not set")
    }

    fun initializeDatabase() {
        val path = Path(LSB_DB_PATH)
        if (!Files.exists(path)) {
            Files.createFile(path)
        }

        DatabaseFactory.init(LSB_DB_PATH)
        LSB_ADMINS_ON_START.forEach { userId -> UserDao.upsertUser(userId, UserRole.ADMIN) }
    }

    fun initializeHandlers() {
        bot = bot {
            token = LSB_BOT_TOKEN
            logLevel = LogLevel.Network.Headers

            dispatch {
                callbackQuery {
                    val data = callbackQuery.data
                    if (data == "noop") {
                        bot.answerCallbackQuery(
                            callbackQuery.id,
                            text = "Это декоративная кнопка",
                            showAlert = false,
                        )
                        return@callbackQuery
                    }

                    val args = data.split(":")
                    val callbackName = args.getOrNull(0) ?: return@callbackQuery
                    val callbackItem = CallbackQueries.allCallbackQueries[callbackName] ?: return@callbackQuery

                    bot.answerCallbackQuery(callbackQuery.id)
                    callbackItem.handler(this)
                }

                Commands.allCommands.forEach { cmd ->
                    command(
                        cmd.commandName,
                        withMinRole(cmd.minRole, cmd.handler),
                    )
                }
            }
        }

        indexingService = IndexingService(LSB_LITERATURE_PATH)
    }

    fun startBot() {
        bot.getUpdates(allowedUpdates = listOf("message", "callback_query"))
        bot.startPolling()
    }

    operator fun invoke(callback: BotContext.() -> Unit) = this.apply(callback)
}
