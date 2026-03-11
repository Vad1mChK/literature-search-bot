package com.vad1mchk.litsearchbot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.logging.LogLevel
import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.bot.BotContext
import com.vad1mchk.litsearchbot.commands.CallbackQueries
import com.vad1mchk.litsearchbot.commands.Commands
import com.vad1mchk.litsearchbot.commands.withMinRole
import com.vad1mchk.litsearchbot.database.DatabaseFactory
import com.vad1mchk.litsearchbot.database.UserDao
import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory
import kotlin.collections.emptyList

fun main() {
    BotContext {
        initializeDatabase()
        initializeHandlers()
        startBot()
    }
//    val env = dotenv()
//    val logger = LoggerFactory.getLogger("MainKt")
//
//    DatabaseFactory.init(env["LSB_DB_PATH"])
//    (env["LSB_ADMINS_ON_START"]?.split(Regex(",\\s*").toPattern()) ?: emptyList())
//        .mapNotNull { str -> str.toLongOrNull() }
//        .forEach { userId -> UserDao.upsertUser(userId, UserRole.ADMIN) }
//
//    val botToken = env["LSB_BOT_TOKEN"] ?: throw IllegalArgumentException("Bot token not found!")
//    val bot = bot {
//        token = botToken
//        logLevel = LogLevel.Network.Body
//
//        dispatch {
//            callbackQuery {
//                val data = callbackQuery.data
//                if (data == "noop") {
//                    bot.answerCallbackQuery(
//                        callbackQuery.id,
//                        text = "Это декоративная кнопка",
//                        showAlert = false,
//                    )
//                    return@callbackQuery
//                }
//
//                val args = data.split(":")
//                val callbackName = args.getOrNull(0) ?: return@callbackQuery
//                val callbackItem = CallbackQueries.allCallbackQueries[callbackName] ?: return@callbackQuery
//
//                bot.answerCallbackQuery(callbackQuery.id)
//                callbackItem.handler(this)
//            }
//
//            Commands.allCommands.forEach { cmd ->
//                logger.info("Registered command: `${cmd.commandName}`")
//                command(
//                    cmd.commandName,
//                    withMinRole(cmd.minRole, cmd.handler),
//                )
//            }
//        }
//    }
//
//    logger.info("Starting bot...")
//    bot.getUpdates(allowedUpdates = listOf("message", "callback_query"))
//    bot.startPolling()
}
