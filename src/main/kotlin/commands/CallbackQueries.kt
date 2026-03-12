package com.vad1mchk.litsearchbot.commands

import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.files.File
import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.bot.BotContext
import com.vad1mchk.litsearchbot.database.IndexedDocumentsDao
import com.vad1mchk.litsearchbot.util.CallbackQueryHandler
import com.vad1mchk.litsearchbot.util.escapeMarkdownV2
import com.vad1mchk.litsearchbot.util.toChatId
import java.nio.file.Files
import kotlin.io.path.Path

object CallbackQueries {
    private val listUsers: CallbackQueryHandler = callback@{
        val args = callbackQuery.data.split(":")
        val offset = args.getOrNull(1)?.toIntOrNull() ?: return@callback

        showUsersPageFromCallback(offset)
    }

    private val listRegisterRequests: CallbackQueryHandler = callback@{
        val args = callbackQuery.data.split(":")
        val offset = args.getOrNull(1)?.toIntOrNull() ?: return@callback

        showRegisterRequestsPageFromCallback(offset)
    }

    private val searchDocs: CallbackQueryHandler = callback@{
        showSearchPageFromCallback()
    }

    private val downloadDoc: CallbackQueryHandler = callback@{
        val callbackMessage = callbackQuery.message ?: return@callback

        val args = callbackQuery.data.split(":")
        val fileHash = args.getOrNull(1) ?: return@callback

        val document = IndexedDocumentsDao.getByHashsum(fileHash)

        if (document == null) {
            bot.sendMessage(
                chatId = callbackMessage.chat.id.toChatId(),
                text = "❌ _Запрашиваемый документ не найден в базе данных\\._",
                replyToMessageId = callbackMessage.messageId,
                parseMode = ParseMode.MARKDOWN_V2
            )
            bot.answerCallbackQuery(callbackQuery.id, "Произошла ошибка", showAlert = true)
            return@callback
        }

        val fullPath = Path(BotContext.LSB_LITERATURE_PATH, document.original)
        if (!Files.exists(fullPath)) {
            bot.sendMessage(
                chatId = callbackMessage.chat.id.toChatId(),
                text = "❌ _Запрашиваемый документ не существует на диске\\._",
                replyToMessageId = callbackMessage.messageId,
                parseMode = ParseMode.MARKDOWN_V2
            )
            bot.answerCallbackQuery(callbackQuery.id, "Произошла ошибка", showAlert = true)
            return@callback
        }

        try {
            // Convert Path to File and wrap in TelegramFile
            val fileToUpload = TelegramFile.ByFile(fullPath.toFile())
            print("Path: ${fileToUpload.file.path}")

            bot.sendMessage(
                chatId = callbackMessage.chat.id.toChatId(),
                text = """
                    📄 _Вы запросили документ:_ ${fullPath.toString().replace("\\", "/").escapeMarkdownV2()}
                    ⌛ _Выполняется отправка документа, подождите…_
                """.trimIndent(),
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = callbackMessage.messageId
            )

            if (document.telegramFileId != null) {
                bot.sendDocument(
                    chatId = callbackMessage.chat.id.toChatId(),
                    document = TelegramFile.ByFileId(document.telegramFileId),
                    caption = "📄 *${document.original.escapeMarkdownV2()}*",
                    parseMode = ParseMode.MARKDOWN_V2,
                    replyToMessageId = callbackMessage.messageId
                )
            } else {
                // 2. Fallback to upload + save file_id
                bot.sendChatAction(chatId = callbackMessage.chat.id.toChatId(), action = ChatAction.UPLOAD_DOCUMENT)

                val fileToUpload = TelegramFile.ByFile(fullPath.toFile())
                val response = bot.sendDocument(
                    chatId = callbackMessage.chat.id.toChatId(),
                    document = fileToUpload,
                    caption = "📄 *${document.original.escapeMarkdownV2()}*",
                    parseMode = ParseMode.MARKDOWN_V2,
                    replyToMessageId = callbackMessage.messageId
                )

                // Extract and save file_id from the successful response
                response.first?.body()?.result?.document?.fileId?.let { newId ->
                    IndexedDocumentsDao.updateFileId(fileHash, newId)
                }
            }
        } catch (e: Exception) {
            bot.sendMessage(
                chatId = callbackMessage.chat.id.toChatId(),
                text = "❌ _Документ был найден, но его не удалось отправить\\._",
                replyToMessageId = callbackMessage.messageId,
                parseMode = ParseMode.MARKDOWN_V2
            )
            bot.answerCallbackQuery(callbackQuery.id, "Произошла ошибка", showAlert = true)
            return@callback
        }

        // It's good practice to answer the callback query to remove the loading spinner
        bot.answerCallbackQuery(callbackQuery.id)
    }

    val allCallbackQueries = listOf(
        BotCallbackQuery("searchDocs", UserRole.REGULAR, searchDocs),
        BotCallbackQuery("downloadDoc", UserRole.REGULAR, downloadDoc),
        BotCallbackQuery("listUsers", UserRole.ADMIN, listUsers),
        BotCallbackQuery("listRegisterRequests", UserRole.ADMIN, listRegisterRequests),
    ).associateBy { it.dataPrefix }
}
