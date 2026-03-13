package com.vad1mchk.litsearchbot.commands

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.vad1mchk.litsearchbot.auth.RegisterStatus
import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.bot.BotContext
import com.vad1mchk.litsearchbot.database.RegisterRequestDao
import com.vad1mchk.litsearchbot.database.UserDao
import com.vad1mchk.litsearchbot.util.CommandHandler
import com.vad1mchk.litsearchbot.util.breakdownCommand
import com.vad1mchk.litsearchbot.util.escapeMarkdownV2
import com.vad1mchk.litsearchbot.util.fullName
import com.vad1mchk.litsearchbot.util.localizeRole
import com.vad1mchk.litsearchbot.util.toChatId

object Commands {
    private object Helpers {
        @JvmStatic
        fun toHelpString(command: BotCommand): String {
            val localizedRole = localizeRole(command.minRole)
            val argStr = command.args?.let { " ${it.escapeMarkdownV2()}" } ?: ""
            return """
                |`${command.commandName.escapeMarkdownV2()}$argStr`
                |*Минимальная роль:* $localizedRole
                |*Описание:*
                |${(command.detailedDescription ?: command.description.escapeMarkdownV2())}
                """.trimMargin()
        }
    }

    private val start: CommandHandler = command@{
        val text = "_${"Добро пожаловать, я бот по поиску литературы!".escapeMarkdownV2()}_"
        val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
            listOf(
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "🎃",
                        callbackData = "pumpkin",
                    ),
                    InlineKeyboardButton.CallbackData(
                        text = "👻",
                        callbackData = "ghost",
                    ),
                    InlineKeyboardButton.CallbackData(
                        text = "🏗",
                        callbackData = "crane",
                    ),
                ),
            ),
        )
        bot.sendMessage(
            message.chat.id.toChatId(),
            text,
            replyToMessageId = message.messageId,
            parseMode = ParseMode.MARKDOWN_V2,
            replyMarkup = inlineKeyboardMarkup,
        )
    }

    private val help: CommandHandler = command@{
        val fromUser = message.from
        if (fromUser == null) {
            bot.sendMessage(
                message.chat.id.toChatId(),
                "_" + "Ошибка: отправитель команды неизвестен.".escapeMarkdownV2() + "_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        val argsList = message.text?.breakdownCommand()
        if (argsList.isNullOrEmpty()) {
            bot.sendMessage(
                message.chat.id.toChatId(),
                "_" + "Ошибка: при распознавании команды не предоставлен текст.".escapeMarkdownV2() + "_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }
        if (argsList.size > 1) {
            val commandName = argsList[1]
            val commandCandidate = allCommands.find { cmd -> cmd.commandName == commandName }
            if (commandCandidate == null) {
                bot.sendMessage(
                    message.chat.id.toChatId(),
                    "_Команда с названием:_ `" + commandName.escapeMarkdownV2() + "` _не найдена\\._",
                    replyToMessageId = message.messageId,
                    parseMode = ParseMode.MARKDOWN_V2,
                )
                return@command
            }

            bot.sendMessage(
                message.chat.id.toChatId(),
                Helpers.toHelpString(commandCandidate),
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        val currentRole = UserDao.getRole(fromUser.id) ?: UserRole.GUEST
        val helpText = (
            "*Список доступных команд:*\n" +
                allCommands
                    .filter { cmd -> cmd.permitsRole(currentRole) }
                    .sortedWith(compareBy<BotCommand> { it.minRole }.thenBy { it.commandName })
                    .map { cmd ->
                        "/${cmd.commandName.escapeMarkdownV2()}${cmd.args?.let { " `${it.escapeMarkdownV2()}`" } ?: ""}: " +
                            cmd.description.escapeMarkdownV2()
                    }
                    .joinToString("\n")
        )
        bot.sendMessage(
            message.chat.id.toChatId(),
            helpText,
            replyToMessageId = message.messageId,
            parseMode = ParseMode.MARKDOWN_V2,
        )
    }

    private val whoami: CommandHandler = command@{
        val fromUser = message.from
        if (fromUser == null) {
            bot.sendMessage(
                message.chat.id.toChatId(),
                "_" + "Ошибка: отправитель команды неизвестен.".escapeMarkdownV2() + "_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        val userExists = UserDao.existsUser(fromUser.id)
        val userRole = UserDao.getRole(fromUser.id) ?: UserRole.GUEST

        val text = """
        |*Информация о пользователе*:
        |\- ID: ${fromUser.id}
        |\- Никнейм: ${fromUser.username?.escapeMarkdownV2() ?: "неизвестно"}
        |\- Полное имя: ${(fromUser.fullName).escapeMarkdownV2()}
        |\- Роль: ${localizeRole(userRole).escapeMarkdownV2()}
        |
        |Данные об этом пользователе *${if (userExists) "сохранены" else "не сохранены"}*\.
        """.trimMargin()
        bot.sendMessage(
            chatId = message.chat.id.toChatId(),
            text = text,
            replyToMessageId = message.messageId,
            parseMode = ParseMode.MARKDOWN_V2,
        )
    }

    private val register: CommandHandler = command@{
        val fromUser = message.from
        if (fromUser == null) {
            bot.sendMessage(
                message.chat.id.toChatId(),
                "_${"Ошибка: отправитель команды неизвестен.".escapeMarkdownV2()}_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        // 1. Check role — already registered users cannot submit a request
        val userRole = UserDao.getRole(fromUser.id) ?: UserRole.GUEST
        if (userRole != UserRole.GUEST) {
            bot.sendMessage(
                message.chat.id.toChatId(),
                "_${"Вы уже зарегистрированы в боте.".escapeMarkdownV2()}_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        // 2. Check for existing pending request
        val existing = RegisterRequestDao.findPending(fromUser.id)
        if (existing != null) {
            bot.sendMessage(
                message.chat.id.toChatId(),
                "_${"Ваш запрос уже находится на рассмотрении.".escapeMarkdownV2()}_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        val username = fromUser.username?.escapeMarkdownV2() ?: "неизвестно"
        val fullName = fromUser.fullName.escapeMarkdownV2()

        // 3. Create request
        val requestId = RegisterRequestDao.create(
            userId = fromUser.id,
            handle = fromUser.username,
            name = fromUser.fullName,
        )

        // 4. Notify the user
        bot.sendMessage(
            message.chat.id.toChatId(),
            "_${"Ваш запрос отправлен администраторам.".escapeMarkdownV2()}_",
            replyToMessageId = message.messageId,
            parseMode = ParseMode.MARKDOWN_V2,
        )

        // 5. Notify admins
        val admins = UserDao.listUsers(userRole = UserRole.ADMIN)
        admins.forEach { admin ->
            bot.sendMessage(
                admin.userId.toChatId(),
                """
                |*Новый запрос на регистрацию*
                |ID: `${fromUser.id}`
                |Имя: $fullName
                |Никнейм: @$username
                |
                |Используйте `/a\_approve\_req $requestId` или `/a\_reject\_req $requestId`
                """.trimMargin(),
                parseMode = ParseMode.MARKDOWN_V2,
            )
        }
    }

    private val search: CommandHandler = command@{
        // TODO implement properly and add button menu
        val searchQuery = message.text?.breakdownCommand(maxArgs = 0)?.getOrNull(1)?.trim()
        if (searchQuery.isNullOrBlank()) {
            bot.sendMessage(
                message.chat.id.toChatId(),
                "_${"Укажите поисковой запрос после названия команды.".escapeMarkdownV2()}_",
                replyToMessageId = message.messageId,
                parseMode = ParseMode.MARKDOWN_V2,
            )
            return@command
        }

        showSearchPage(searchQuery)
    }

    private val adminListUsers: CommandHandler = command@{
        showUsersPage(
            offset = 0,
        )
    }

    private val adminListReqs: CommandHandler = command@{
        showRegisterRequestsPage(
            offset = 0,
        )
    }

    private val adminApproveReq: CommandHandler = command@{
        val adminUser = message.from ?: return@command

        val args = message.text?.breakdownCommand() ?: emptyList()
        val id = args.getOrNull(1)?.toIntOrNull()

        if (id == null) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Укажите ID запроса: `/a\\_approve\\_req <id>`_",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        // 1. Lookup request before modifying it
        val req = RegisterRequestDao.getById(id)
        if (req == null || req.status != RegisterStatus.PENDING) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Запрос с ID `$id` не найден или уже был обработан\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        // 2. Update DB: approve request
        RegisterRequestDao.approve(id, adminUser.id)

        // 3. Register the user in the system
        if (!UserDao.existsUser(req.userId) || UserDao.getById(req.userId)?.role == UserRole.GUEST) {
            UserDao.upsertUser(req.userId, UserRole.REGULAR, req.handle)
        } else {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Пользователь с ID ${req.userId} уже зарегистрирован в системе\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        // 4. Notify the requester
        bot.sendMessage(
            chatId = req.userId.toChatId(),
            text = "_Ваш запрос был одобрен администратором\\._",
            parseMode = ParseMode.MARKDOWN_V2,
        )

        // 5. Notify the approving admin (confirmation message)
        bot.sendMessage(
            chatId = message.chat.id.toChatId(),
            text = "_Вы одобрили запрос `${id}\\._",
            parseMode = ParseMode.MARKDOWN_V2,
            replyToMessageId = message.messageId,
        )
    }

    private val adminRejectReq: CommandHandler = command@{
        val adminUser = message.from ?: return@command

        val args = message.text?.breakdownCommand(maxArgs = 1) ?: emptyList()
        val id = args.getOrNull(1)?.toIntOrNull()
        val reason = args.getOrNull(2)?.ifBlank { null }

        if (id == null) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Укажите ID запроса: `/a\\_reject\\_req <id>`_",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        // 1. Lookup request before modifying it
        val req = RegisterRequestDao.getById(id)
        if (req == null || req.status != RegisterStatus.PENDING) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Запрос с ID `$id` не найден или уже был обработан\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        // 2. Update DB: approve request
        RegisterRequestDao.reject(id, adminUser.id, reason = args.getOrNull(2))

        // 4. Notify the requester
        bot.sendMessage(
            chatId = req.userId.toChatId(),
            text = "_Ваш запрос отклонён администратором\\. Причина: ${reason?.escapeMarkdownV2() ?: "не указана"}_",
            parseMode = ParseMode.MARKDOWN_V2,
        )

        // 5. Notify the approving admin (confirmation message)
        bot.sendMessage(
            chatId = message.chat.id.toChatId(),
            text = "_Вы отклонили запрос `$id`\\. Причина: ${reason?.escapeMarkdownV2() ?: "не указана"}_",
            parseMode = ParseMode.MARKDOWN_V2,
            replyToMessageId = message.messageId,
        )
    }

    private val adminRegisterUser: CommandHandler = command@{
        val args = message.text?.breakdownCommand() ?: emptyList()
        val userId = args.getOrNull(1)?.toLongOrNull()

        if (userId == null) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Укажите ID пользователя: `/a\\_register <id>`_",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        if (userId == message.from?.id) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Указанный ID пользователя `$userId` совпадает с Вашим\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        val currentRole = UserDao.getRole(userId)
        if (currentRole != null && currentRole != UserRole.GUEST) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Пользователь с ID `$userId` уже зарегистрирован в системе\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        UserDao.upsertUser(userId, UserRole.REGULAR)

        runCatching {
            bot.sendMessage(
                chatId = userId.toChatId(),
                text = "_Вы были зарегистрированы в боте\\._",
                parseMode = ParseMode.MARKDOWN_V2,
            )
        }.onFailure {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Пользователь `$userId` зарегистрирован, но уведомление не отправлено\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
        }

        bot.sendMessage(
            chatId = message.chat.id.toChatId(),
            text = "_Вы зарегистрировали пользователя `$userId`\\._",
            parseMode = ParseMode.MARKDOWN_V2,
            replyToMessageId = message.messageId,
        )
    }

    private val adminUnregisterUser: CommandHandler = command@{
        val args = message.text?.breakdownCommand() ?: emptyList()
        val userId = args.getOrNull(1)?.toLongOrNull()

        if (userId == null) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Укажите ID пользователя: `/a\\_unregister <id>`_",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        if (userId == message.from?.id) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Указанный ID пользователя `$userId` совпадает с Вашим\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        val currentRole = UserDao.getRole(userId)
        if (currentRole == null) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Пользователь с ID `$userId` не зарегистрирован в системе\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        if (currentRole == UserRole.ADMIN) {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Нельзя разрегистрировать администратора\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
            return@command
        }

        UserDao.deleteUser(userId)

        runCatching {
            bot.sendMessage(
                chatId = userId.toChatId(),
                text = "_Ваша регистрация в боте была аннулирована\\._",
                parseMode = ParseMode.MARKDOWN_V2,
            )
        }.onFailure {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                text = "_Пользователь `$userId` разрегистрирован, но уведомление не отправлено\\._",
                parseMode = ParseMode.MARKDOWN_V2,
                replyToMessageId = message.messageId,
            )
        }

        bot.sendMessage(
            chatId = message.chat.id.toChatId(),
            text = "_Вы разрегистрировали пользователя `$userId`\\._",
            parseMode = ParseMode.MARKDOWN_V2,
            replyToMessageId = message.messageId,
        )
    }

    private val adminReindex: CommandHandler = command@{
        val newMessage = bot.sendMessage(
            chatId = message.chat.id.toChatId(),
            text = "⌛ _Реиндексация в процессе_ \\(подготовка…\\)",
            parseMode = ParseMode.MARKDOWN_V2,
            replyToMessageId = message.messageId,
        )
        val newMessageId = newMessage.getOrNull()?.messageId
        var resultMessageText = ""

        try {
            val stats = BotContext.indexingService.reindex { current, total ->
                val emoji = if (current % 2 == 0) "⏳" else "⌛️"
                if (newMessageId != null) {
                    bot.editMessageText(
                        chatId = message.chat.id.toChatId(),
                        messageId = newMessageId,
                        text = "$emoji _Реиндексация в процессе_ \\($current/$total\\)",
                        parseMode = ParseMode.MARKDOWN_V2,
                    )
                }
            }
            resultMessageText = """
            |✅ _Реиндексация завершена\._
            |\- Актуальные: ${stats.upToDate}
            |\- Обновлено: ${stats.updated}
            |\- Добавлено: ${stats.added}
            |\- Удалено: ${stats.deleted}
            |\- Не удалось обработать: ${stats.failed}
            """.trimMargin()
        } catch (e: Exception) {
            resultMessageText = "❌ _При индексации произошла ошибка:_ `${
                e.message?.escapeMarkdownV2() ?: "неизвестная ошибка"
            }`"
        }

        if (newMessageId != null) {
            bot.editMessageText(
                chatId = message.chat.id.toChatId(),
                messageId = newMessageId,
                text = resultMessageText,
                parseMode = ParseMode.MARKDOWN_V2,
            )
        } else {
            bot.sendMessage(
                chatId = message.chat.id.toChatId(),
                replyToMessageId = message.messageId,
                text = resultMessageText,
                parseMode = ParseMode.MARKDOWN_V2,
            )
        }
    }

    private val adminIndexInfo: CommandHandler = command@{
        val stats = BotContext.indexingService.computeTotalStats()
        val statsByCategory = mapOf(
            "Актуальные" to stats.upToDate,
            "Устаревшие" to stats.outdated,
            "Исчезнувшие" to stats.disappeared,
            "Не индексированы" to stats.unindexed,
        )
            .filter { it.value != 0 }
            .map { "\\- ${it.key}: ${it.value}" }
            .joinToString("\n")

        val warningText = if (stats.disappeared != 0 || stats.unindexed != 0) {
            "⚠ Список файлов в индексе отличается от списка файлов на диске!"
        } else if (stats.outdated != 0) {
            "⚠ Представления некоторых файлов в индексе устарели!"
        } else {
            null
        }
        val reindexText = "Запустите реиндексацию командой /a_reindex."

        val resultMessageText = """
        |🏗 _Информация об индексе:_
        |$statsByCategory
        |
        |\- Всего файлов на диске: ${stats.totalOnDisk}
        |\- Всего файлов в базе данных: ${stats.totalInDatabase}
        |
        |${
            if (warningText != null) {
                ("> *${warningText.escapeMarkdownV2()}*\n> *${reindexText.escapeMarkdownV2()}*")
            } else {
                ""
            }
        }
        """.trimMargin()

        bot.sendMessage(
            chatId = message.chat.id.toChatId(),
            text = resultMessageText,
            parseMode = ParseMode.MARKDOWN_V2,
            replyToMessageId = message.messageId,
        )
    }

    @JvmStatic
    val allCommands = listOf(
        BotCommand(
            "start",
            "Начать работу с ботом.",
            minRole = UserRole.GUEST,
            handler = Commands.start,
        ),
        BotCommand(
            "help",
            "Вывести список доступных команд или справку по конкретной команде.",
            minRole = UserRole.GUEST,
            handler = Commands.help,
            args = "[help]",
            detailedDescription = """
                |При вызове без аргументов \(`/help`\) выводит список всех команд, доступных пользователю\.
                |При вызове с именем команды \(`/help \<cmd\>`\) выводит более детальную справку по конкретной команде\.
            """.trimMargin(),
        ),
        BotCommand(
            "whoami",
            "Вывести краткую информацию о текущем пользователе.",
            minRole = UserRole.GUEST,
            handler = Commands.whoami,
        ),
        BotCommand(
            "register",
            "Отправить запрос на регистрацию.",
            minRole = UserRole.GUEST,
            handler = Commands.register,
            detailedDescription = """
                |Отправить запрос на регистрацию в системе\. 
                |Далее запрос рассматривает один из администраторов, в случае успеха вы будете зарегистрированы\.
                |Если вы уже зарегистрированы в системе, никаких новых действий не выполняется\.
            """.trimMargin(),
        ),
        BotCommand(
            "search",
            "Поиск с заданным запросом по литературе.",
            minRole = UserRole.REGULAR,
            handler = Commands.search,
            args = "<query...>",
            detailedDescription = """
                |Выполняет поиск по литературе, используя заданный запрос\.
                |Результаты выводятся с пагинацией и кнопками\.
            """.trimMargin(),
        ),
        BotCommand(
            "a_list_users",
            "Вывести список зарегистрированных пользователей.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminListUsers,
        ),
        BotCommand(
            "a_list_reqs",
            "Вывести список запросов на регистрацию.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminListReqs,
        ),
        BotCommand(
            "a_approve_req",
            "Одобрить запрос на регистрацию с указанным ID.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminApproveReq,
            args = "<request_id>",
            detailedDescription = """
                |Одобрить запрос на регистрацию в системе\. 
                |После этого пользователь будет зарегистрирован и получит доступ к базовым функциям\.
                |Если вы уже зарегистрированы в системе, никаких новых действий не выполняется\.
            """.trimMargin(),
        ),
        BotCommand(
            "a_reject_req",
            "Отклонить запрос на регистрацию с указанным ID.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminRejectReq,
            args = "<request_id> [reason...]",
            detailedDescription = """
                |Отклонить запрос на регистрацию в системе\. Можно опционально указать причину\.
                |После этого пользователь сможет отправить запрос ещё раз\.
            """.trimMargin(),
        ),
        BotCommand(
            "a_register_user",
            "Зарегистрировать пользователя с указанным ID.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminRegisterUser,
            args = "<user_id>",
            detailedDescription = """
                |Зарегистрировать пользователя в боте напрямую\. 
                |Нельзя зарегистрировать себя или уже зарегистрированного пользователя\.
            """.trimMargin(),
        ),
        BotCommand(
            "a_unregister_user",
            "Разрегистрировать пользователя с указанным ID.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminUnregisterUser,
            args = "<user_id>",
            detailedDescription = """
                |Разрегистрировать пользователя в боте напрямую\. 
                |Нельзя разрегистрировать себя, администратора или несуществующего пользователя\.
            """.trimMargin(),
        ),
        BotCommand(
            "a_reindex",
            "Реиндексация документов.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminReindex,
            detailedDescription = """
                |Запустить реиндексацию каталога документов с литературой\.
                |Будут добавлены, обновлены или удалены записи индекса\.
            """.trimMargin(),
        ),
        BotCommand(
            "a_index_info",
            "Информация о текущем индексе.",
            minRole = UserRole.ADMIN,
            handler = Commands.adminIndexInfo,
            detailedDescription = """
                |Вывести информацию по текущему индексу и статус записей в нём\.
            """.trimMargin(),
        ),
    )
}
