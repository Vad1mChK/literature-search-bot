package com.vad1mchk.litsearchbot.commands

import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.util.CallbackQueryHandler

/**
 * Entity representing a handled Callback Query.
 * @property dataPrefix The prefix/ID of the callback (e.g., "pumpkin")
 * @property handler The logic to execute
 */
data class BotCallbackQuery(
    val dataPrefix: String,
    val minRole: UserRole,
    val handler: CallbackQueryHandler,
)
