package com.vad1mchk.litsearchbot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.vad1mchk.litsearchbot.auth.RegisterStatus
import com.vad1mchk.litsearchbot.database.IndexedDocumentsDao
import com.vad1mchk.litsearchbot.database.RegisterRequestDao
import com.vad1mchk.litsearchbot.database.SearchQueryLookupDao
import com.vad1mchk.litsearchbot.database.UserDao
import com.vad1mchk.litsearchbot.util.escapeMarkdownV2
import com.vad1mchk.litsearchbot.util.formatSnippetForMarkdownV2
import com.vad1mchk.litsearchbot.util.md5
import com.vad1mchk.litsearchbot.util.toChatId
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.collections.plusAssign
import kotlin.math.min

private const val USERS_PAGE_SIZE = 10
private const val REQS_PAGE_SIZE = 10
private const val SEARCH_PAGE_SIZE = 5
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

    val buttons = buildPaginationButtonsFull(offset, total, key = "listUsers")
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

    val buttons = buildPaginationButtonsFull(offset, total, key = "listRegisterRequests")
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

// In CommandHandler
fun CommandHandlerEnvironment.showSearchPage(query: String) {
    val hash = md5(query) // Your helper to get MD5 string

    // Save to LRU cache so callback can find it later
    SearchQueryLookupDao.upsertQuery(hash, query)

    showSearchPageCommon(
        bot = bot,
        chatId = message.chat.id.toChatId(),
        queryText = query,
        queryHash = hash,
        offset = 0,
        limit = SEARCH_PAGE_SIZE,
        replyToMessageId = message.messageId
    )
}

// In CallbackQueryHandler
// Data format: "searchDocs:<queryHash>:<offset>"
fun CallbackQueryHandlerEnvironment.showSearchPageFromCallback() {
    val args = callbackQuery.data.split(":")
    if (args.size < 3) return

    val queryHash = args[1]
    val offset = args[2].toIntOrNull() ?: 0
    val limit = args[3].toIntOrNull() ?: 10

    // Retrieve the original query text from the database
    val queryText = SearchQueryLookupDao.findByHash(queryHash)

    if (queryText == null) {
        bot.answerCallbackQuery(callbackQuery.id, text = "Сессия поиска истекла. Повторите запрос.", showAlert = true)
        return
    }

    val chatId = callbackQuery.message?.chat?.id ?: return

    showSearchPageCommon(
        bot = bot,
        chatId = chatId.toChatId(),
        queryText = queryText,
        queryHash = queryHash,
        offset = offset,
        limit = limit,
        messageId = callbackQuery.message?.messageId
    )

    bot.answerCallbackQuery(callbackQuery.id)
}

private fun showSearchPageCommon(
    bot: Bot,
    chatId: ChatId,
    queryText: String,
    queryHash: String,
    offset: Int,
    limit: Int = 10,
    messageId: Long? = null,
    replyToMessageId: Long? = null,
) {
    try {
        val total = IndexedDocumentsDao.countSearchResults(queryText)
        val searchResults = IndexedDocumentsDao.searchWithSnippet(queryText, limit = limit, offset = offset)

        val resultHeaderText = """
            |*Результаты поиска по запросу* `${queryText.escapeMarkdownV2()}`:
            |_Показаны результаты с ${offset + 1} по ${minOf(offset + limit, total.toInt())} из ${total}_
            |
            |
        """.trimMargin()
        val resultListText = if (searchResults.isNotEmpty()) {
            searchResults.joinToString("\n") { res ->
                "\\- *${res.second.escapeMarkdownV2()}* \n${formatSnippetForMarkdownV2(res.third)}\n"
            }
        } else {
            "_Поиск не дал результатов\\._"
        }

        // Prepare download list for buttons: List<Pair<Hash, Filename>>
        val downloadHashes = searchResults.map { it.first to it.second }

        val keyboard = buildSearchResultsButtons(
            offset = offset,
            limit = limit,
            total = total.toInt(),
            searchKey = "searchDocs:$queryHash", // Append hash for pagination lookup
            downloadHashes = downloadHashes
        )

        val fullText = resultHeaderText + resultListText

        if (messageId != null) {
            bot.editMessageText(
                chatId = chatId,
                messageId = messageId,
                text = fullText,
                parseMode = ParseMode.MARKDOWN_V2,
                replyMarkup = keyboard
            )
        } else {
            bot.sendMessage(
                chatId = chatId,
                text = fullText,
                replyToMessageId = replyToMessageId,
                parseMode = ParseMode.MARKDOWN_V2,
                replyMarkup = keyboard
            )
        }
    } catch (e: Exception) {
        bot.sendMessage(
            chatId = chatId,
            text = "_При выполнении поиска произошла ошибка\\._",
            replyToMessageId = replyToMessageId,
            parseMode = ParseMode.MARKDOWN_V2
        )
    }
}

fun buildPaginationButtonsFull(offset: Int, total: Int, key: String): InlineKeyboardMarkup {
    val row = buildPaginationButtonsRow(offset = offset, limit = USERS_PAGE_SIZE, total = total, key = key)
    val buttons = listOf(row)

    return InlineKeyboardMarkup.create(buttons)
}

fun buildSearchResultsButtons(
    offset: Int,
    limit: Int,
    total: Int,
    searchKey: String = "searchDocs",
    downloadKey: String = "downloadDoc",
    downloadHashes: List<Pair<String, String>> = emptyList(),
): InlineKeyboardMarkup {
    val buttons = mutableListOf<List<InlineKeyboardButton>>()

    if (total > SEARCH_PAGE_SIZE) {
        val paginationRow = buildPaginationButtonsRow(
            offset = offset,
            limit = limit,
            total = total,
            key = searchKey,
            showLimit = true,
        )

        buttons += paginationRow
    }

    downloadHashes.forEach { (hash, path) ->
        val button = InlineKeyboardButton.CallbackData(
            text = "⬇ $path",
            callbackData = "$downloadKey:$hash"
        )
        buttons += listOf(button)
    }

    return InlineKeyboardMarkup.create(buttons)
}

private fun buildPaginationButtonsRow(
    offset: Int, limit: Int, total: Int, key: String, showLimit: Boolean = false
): List<InlineKeyboardButton> {
    val prevOffset = maxOf(offset - limit, 0)
    val nextOffset = offset + limit

    val row = mutableListOf<InlineKeyboardButton>()

    row += if (offset > 0) {
        InlineKeyboardButton.CallbackData(
            text = "◀",
            callbackData = if (showLimit) "$key:$prevOffset:$limit" else "$key:$prevOffset",
        )
    } else {
        InlineKeyboardButton.CallbackData(text = "🚫", callbackData = "noop")
    }

    if (total > limit) {
        row += InlineKeyboardButton.CallbackData(
            text = "${offset + 1}…${min(nextOffset, total)}",
            callbackData = "noop",
        )
    }

    row += if (nextOffset < total) {
        InlineKeyboardButton.CallbackData(
            text = "▶",
            callbackData = if (showLimit) "$key:$nextOffset:$limit" else "$key:$nextOffset",
        )
    } else {
        InlineKeyboardButton.CallbackData(text = "🚫", callbackData = "noop")
    }

    return row
}
