package com.vad1mchk.litsearchbot.commands

import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.util.CommandHandler

/**
 * Entity representing a Telegram command a bot can execute.
 * @property commandName Telegram command name (after `/`)
 * @property description Short description, shown in bot menu and `/help`"` without args
 * @property minRole Minimum role required to access this command
 * @property handler Command handler (coroutine)
// * @property argsCount (optional) Count of positional args before the trailing unlimited-length arg
 * @property args Command args notation, e.g. `<arg>`
 * @property detailedDescription Description for detailed help (`/help <commandName>`),
 * guaranteed to be Markdown-safe if present
 */
data class BotCommand(
    val commandName: String,
    val description: String,
    val minRole: UserRole,
    val handler: CommandHandler,
    val args: String? = null,
    val detailedDescription: String? = null,
) {
    fun permitsRole(currentRole: UserRole): Boolean {
        return currentRole.ordinal >= minRole.ordinal
    }
}
