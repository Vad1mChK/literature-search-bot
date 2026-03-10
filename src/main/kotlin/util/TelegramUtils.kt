package com.vad1mchk.litsearchbot.util

import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User

typealias CommandHandler = suspend CommandHandlerEnvironment.() -> Unit

typealias CallbackQueryHandler = CallbackQueryHandlerEnvironment.() -> Unit

fun Long.toChatId(): ChatId = ChatId.fromId(this)

val User.fullName get() = "$firstName${lastName?.let { " $it" } ?: ""}"
