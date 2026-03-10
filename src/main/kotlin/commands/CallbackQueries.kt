package com.vad1mchk.litsearchbot.commands

import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.util.CallbackQueryHandler

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

    val allCallbackQueries = listOf(
        BotCallbackQuery("listUsers", UserRole.ADMIN, listUsers),
        BotCallbackQuery("listRegisterRequests", UserRole.ADMIN, listRegisterRequests),
    ).associateBy { it.dataPrefix }
}
