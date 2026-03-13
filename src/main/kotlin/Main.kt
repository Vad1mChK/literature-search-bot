package com.vad1mchk.litsearchbot

import com.vad1mchk.litsearchbot.bot.BotContext

fun main() {
    BotContext {
        initializeDatabase()
        initializeHandlers()
        startBot()
    }
}
