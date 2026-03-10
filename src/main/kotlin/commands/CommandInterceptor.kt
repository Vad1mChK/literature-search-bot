package com.vad1mchk.litsearchbot.commands

import com.github.kotlintelegrambot.entities.ParseMode
import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.database.RegisterRequestDao
import com.vad1mchk.litsearchbot.database.UserDao
import com.vad1mchk.litsearchbot.util.CommandHandler
import com.vad1mchk.litsearchbot.util.escapeMarkdownV2
import com.vad1mchk.litsearchbot.util.fullName
import com.vad1mchk.litsearchbot.util.toChatId

fun withMinRole(
    minRole: UserRole,
    handler: CommandHandler,
): CommandHandler {
    return command@{
        val user = message.from
        if (user == null) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_" + "Ошибка: отправитель команды неизвестен.".escapeMarkdownV2() + "_",
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        // Update last known handle and name
        if (UserDao.existsUser(user.id)) {
            UserDao.updateHandle(user.id, user.username)
        } else {
            RegisterRequestDao.updateHandleAndName(user.id, user.username, user.fullName)
        }

        val role = UserDao.getRole(user.id) ?: UserRole.GUEST

        if (role.ordinal < minRole.ordinal) {
            val text = if (minRole == UserRole.REGULAR) {
                "Отказано в доступе. Для получения доступа запросите регистрацию с помощью команды /register."
            } else {
                "Отказано в доступе."
            }

            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_" + text.escapeMarkdownV2() + "_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        handler(this)
    }
}
