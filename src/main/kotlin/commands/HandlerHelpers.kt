package com.vad1mchk.litsearchbot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.vad1mchk.litsearchbot.util.escapeMarkdownV2
import com.vad1mchk.litsearchbot.util.localizeRole
import com.vad1mchk.litsearchbot.util.toChatId

fun BotCommand.toHelpString(): String {
    val localizedRole = localizeRole(this.minRole)
    val argStr = this.args?.let { " ${it.escapeMarkdownV2()}" } ?: ""
    return """
        |`${this.commandName.escapeMarkdownV2()}$argStr`
        |*Минимальная роль:* $localizedRole
        |*Описание:*
        |${(this.detailedDescription ?: this.description.escapeMarkdownV2())}
        """.trimMargin()
}

fun Bot.sendSafeReply(
    chatId: ChatId,
    replyToId: Long?,
    text: String,
    parseMode: ParseMode? = ParseMode.MARKDOWN_V2,
    replyMarkup: ReplyMarkup? = null,
) = sendMessage(
    chatId = chatId,
    text = text,
    parseMode = parseMode,
    replyToMessageId = replyToId,
    replyMarkup = replyMarkup,
    disableWebPagePreview = true,
)

fun CommandHandlerEnvironment.sendReplyMessage(
    text: String,
    replyMarkup: ReplyMarkup? = null,
) = bot.sendSafeReply(
    chatId = message.chat.id.toChatId(),
    replyToId = message.messageId,
    text = text,
    replyMarkup = replyMarkup,
)

fun CallbackQueryHandlerEnvironment.sendReplyMessage(
    text: String,
    replyMarkup: ReplyMarkup? = null,
) = callbackQuery.message?.let { callbackMessage ->
    bot.sendSafeReply(
        chatId = callbackMessage.chat.id.toChatId(),
        replyToId = callbackMessage.messageId,
        text = text,
        replyMarkup = replyMarkup,
    )
}
