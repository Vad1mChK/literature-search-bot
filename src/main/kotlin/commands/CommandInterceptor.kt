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
        val tgUser = message.from
        if (tgUser == null) {
            sendReplyMessage("_${"Ошибка: отправитель команды неизвестен.".escapeMarkdownV2()}_")
            return@command
        }

        val userEntity = UserDao.getById(tgUser.id)

        // Update last known handle and name
        if (userEntity != null) {
            UserDao.updateHandle(tgUser.id, tgUser.username)
        } else {
            RegisterRequestDao.updateHandleAndName(tgUser.id, tgUser.username, tgUser.fullName)
        }

        val role = userEntity?.role ?: UserRole.GUEST

        if (role.ordinal < minRole.ordinal) {
            val errorText = when (minRole) {
                UserRole.REGULAR ->
                    "Отказано в доступе. Для получения доступа запросите регистрацию с помощью команды /register."
                else -> "Отказано в доступе."
            }
            sendReplyMessage("_${errorText.escapeMarkdownV2()}_")
            return@command
        }

        handler(this)
    }
}
