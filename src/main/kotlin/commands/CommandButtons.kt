package com.vad1mchk.litsearchbot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.vad1mchk.litsearchbot.auth.RegisterStatus
import com.vad1mchk.litsearchbot.database.RegisterRequestDao
import com.vad1mchk.litsearchbot.database.UserDao
import com.vad1mchk.litsearchbot.util.escapeMarkdownV2
import com.vad1mchk.litsearchbot.util.toChatId
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.collections.plusAssign
import kotlin.math.min

private const val USERS_PAGE_SIZE = 10
private const val REQS_PAGE_SIZE = 10
private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun CommandHandlerEnvironment.showUsersPage(offset: Int) {
    showUsersPageCommon(
        bot = bot,
        chatId = message.chat.id.toChatId(),
        offset = offset,
        replyToMessageId = message.messageId,
    )
}

fun CallbackQueryHandlerEnvironment.showUsersPageFromCallback(offset: Int) {
    val chatId = callbackQuery.message?.chat?.id ?: return
    showUsersPageCommon(
        bot = bot,
        chatId = chatId.toChatId(),
        offset = offset,
        messageId = callbackQuery.message?.messageId,
        replyToMessageId = callbackQuery.message?.messageId,
    )
}

private fun showUsersPageCommon(
    bot: Bot,
    chatId: ChatId,
    offset: Int,
    messageId: Long? = null,
    replyToMessageId: Long? = null,
) {
    val users = UserDao.listUsers(limit = USERS_PAGE_SIZE, offset = offset)
    val total = UserDao.countUsers().toInt()

    if (users.isEmpty()) {
        val emptyText = "_Пользователи отсутствуют_\\."
        if (messageId != null) {
            bot.editMessageText(chatId, messageId, text = emptyText, parseMode = ParseMode.MARKDOWN_V2)
        } else {
            bot.sendMessage(chatId, text = emptyText, replyToMessageId = replyToMessageId, parseMode = ParseMode.MARKDOWN_V2)
        }
        return
    }

    val header = "*Список пользователей*\n" +
        "_Всего:_ $total\n" +
        "_Показаны:_ ${offset + 1}…${minOf(offset + users.size, total)}\n\n"

    val body = users.joinToString("\n") { u ->
        val handle = u.lastKnownHandle?.escapeMarkdownV2() ?: "\\(нет\\)"
        val role = u.role.name.escapeMarkdownV2()
        "• `${u.userId}`, @$handle, $role"
    }

    val buttons = buildPaginationButtons(offset, total, key = "listUsers")
    val finalText = header + body

    if (messageId != null) {
        bot.editMessageText(
            chatId = chatId,
            messageId = messageId,
            text = finalText,
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = buttons,
        )
    } else {
        bot.sendMessage(
            chatId = chatId,
            text = finalText,
            replyToMessageId = replyToMessageId,
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = buttons,
        )
    }
}

fun CommandHandlerEnvironment.showRegisterRequestsPage(offset: Int) {
    showRegisterRequestsPageCommon(
        bot = bot,
        chatId = message.chat.id.toChatId(),
        offset = offset,
        replyToMessageId = message.messageId,
    )
}

fun CallbackQueryHandlerEnvironment.showRegisterRequestsPageFromCallback(offset: Int) {
    val chatId = callbackQuery.message?.chat?.id ?: return
    showRegisterRequestsPageCommon(
        bot = bot,
        chatId = chatId.toChatId(),
        offset = offset,
        messageId = callbackQuery.message?.messageId,
        replyToMessageId = callbackQuery.message?.messageId,
    )
}

private fun showRegisterRequestsPageCommon(
    bot: Bot,
    chatId: ChatId,
    offset: Int,
    messageId: Long? = null,
    replyToMessageId: Long? = null,
) {
    val requests = RegisterRequestDao.list(offset = offset, limit = REQS_PAGE_SIZE, status = RegisterStatus.PENDING)
    val total = RegisterRequestDao.count(status = RegisterStatus.PENDING).toInt()

    if (requests.isEmpty()) {
        val emptyText = "_Необработанные запросы на регистрацию отсутствуют_\\."
        if (messageId != null) {
            bot.editMessageText(chatId, messageId, text = emptyText, parseMode = ParseMode.MARKDOWN_V2)
        } else {
            bot.sendMessage(chatId, text = emptyText, replyToMessageId = replyToMessageId, parseMode = ParseMode.MARKDOWN_V2)
        }
        return
    }

    val header = "*Список запросов*\n" +
        "_Всего:_ $total\n" +
        "_Показаны:_ ${offset + 1}…${minOf(offset + requests.size, total)}\n\n"

    val body = requests.joinToString("\n") { r ->
        val handle = r.handle?.escapeMarkdownV2() ?: "\\(нет\\)"
        val date = Instant.ofEpochMilli(r.createdAt).atZone(ZoneOffset.UTC).toLocalDateTime()
        val formattedDateTime = DATE_FORMAT.format(date).escapeMarkdownV2()
        "• requestId: `${r.id}`, userId: `${r.userId}`, @$handle, в $formattedDateTime GMT"
    }

    val footer = "\n\n*Чтобы одобрить/отклонить запрос, используйте команду:* " +
        "`/a\\_approve\\_req \\<request\\_id\\>` " +
        "*или* " +
        "`/a\\_reject\\_req \\<request\\_id\\>`\\."

    val buttons = buildPaginationButtons(offset, total, key = "listRegisterRequests")
    val finalText = header + body + footer

    if (messageId != null) {
        bot.editMessageText(
            chatId = chatId,
            messageId = messageId,
            text = finalText,
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = buttons,
        )
    } else {
        bot.sendMessage(
            chatId = chatId,
            text = finalText,
            replyToMessageId = replyToMessageId,
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = buttons,
        )
    }
}

fun buildPaginationButtons(offset: Int, total: Int, key: String): InlineKeyboardMarkup {
    val prevOffset = maxOf(offset - USERS_PAGE_SIZE, 0)
    val nextOffset = offset + USERS_PAGE_SIZE

    val buttons = mutableListOf<List<InlineKeyboardButton>>()

    val row = mutableListOf<InlineKeyboardButton>()

    row += if (offset > 0) {
        InlineKeyboardButton.CallbackData(
            text = "◀",
            callbackData = "$key:$prevOffset",
        )
    } else {
        InlineKeyboardButton.CallbackData(text = "🚫", callbackData = "noop")
    }

    if (total > USERS_PAGE_SIZE) {
        row += InlineKeyboardButton.CallbackData(
            text = "${offset + 1}…${min(nextOffset, total)}",
            callbackData = "noop",
        )
    }

    row += if (nextOffset < total) {
        InlineKeyboardButton.CallbackData(
            text = "▶",
            callbackData = "$key:$nextOffset",
        )
    } else {
        InlineKeyboardButton.CallbackData(text = "🚫", callbackData = "noop")
    }

    if (row.isNotEmpty()) {
        buttons += row
    }

    return InlineKeyboardMarkup.create(buttons)
}
