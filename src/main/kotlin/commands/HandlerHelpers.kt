package com.vad1mchk.litsearchbot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.database.UserDao
import com.vad1mchk.litsearchbot.util.escapeMarkdownV2
import com.vad1mchk.litsearchbot.util.fullName
import com.vad1mchk.litsearchbot.util.localizeRole
import com.vad1mchk.litsearchbot.util.toChatId
import com.github.kotlintelegrambot.entities.User as TelegramUser

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

/**
 * Sends a list of all commands available to the user based on their role.
 */
fun helpHelper(
    bot: Bot,
    chatId: ChatId,
    userId: Long,
    replyToId: Long? = null,
    allCommands: List<BotCommand>,
) {
    val currentRole = UserDao.getRole(userId) ?: UserRole.GUEST
    val helpText = (
        "*Список доступных команд:*\n" +
            allCommands
                .filter { cmd -> cmd.permitsRole(currentRole) }
                .sortedWith(compareBy<BotCommand> { it.minRole }.thenBy { it.commandName })
                .joinToString("\n") { cmd ->
                    "/${cmd.commandName.escapeMarkdownV2()}${cmd.args?.let { " `${it.escapeMarkdownV2()}`" } ?: ""}: " +
                        cmd.description.escapeMarkdownV2()
                }
    )

    bot.sendMessage(
        chatId = chatId,
        text = helpText,
        parseMode = ParseMode.MARKDOWN_V2,
        replyToMessageId = replyToId,
    )
}

/**
 * Sends detailed help for a specific command name.
 */
fun helpForCommandHelper(
    bot: Bot,
    chatId: ChatId,
    commandName: String,
    replyToId: Long? = null,
    allCommands: List<BotCommand>,
) {
    val commandCandidate = allCommands.find { it.commandName == commandName }

    val text = commandCandidate?.toHelpString()
        ?: "_Команда с названием:_ `${commandName.escapeMarkdownV2()}` _не найдена\\._"

    bot.sendMessage(
        chatId = chatId,
        text = text,
        parseMode = ParseMode.MARKDOWN_V2,
        replyToMessageId = replyToId,
    )
}

fun whoamiHelper(
    bot: Bot,
    chatId: ChatId,
    user: TelegramUser,
    replyToId: Long? = null,
) {
    // Optimization: If you have getById, use it to get the role and existence in one go.
    // Otherwise, we stick to your current DAO methods:
    val userEntity = UserDao.getById(user.id)
    val userRole = userEntity?.role ?: UserRole.GUEST

    val text = """
    |*Информация о пользователе*:
    |\- ID: ${user.id}
    |\- Никнейм: ${user.username?.escapeMarkdownV2() ?: "неизвестно"}
    |\- Полное имя: ${user.fullName.escapeMarkdownV2()}
    |\- Роль: ${localizeRole(userRole).escapeMarkdownV2()}
    |
    |Данные об этом пользователе *${if (userEntity != null) "сохранены" else "не сохранены"}*\.
    """.trimMargin()

    bot.sendMessage(
        chatId = chatId,
        text = text,
        parseMode = ParseMode.MARKDOWN_V2,
        replyToMessageId = replyToId,
    )
}
