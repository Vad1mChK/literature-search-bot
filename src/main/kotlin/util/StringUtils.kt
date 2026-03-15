package com.vad1mchk.litsearchbot.util

import com.vad1mchk.litsearchbot.auth.UserRole
import java.math.BigInteger
import java.security.MessageDigest

private val TG_COMMAND_REGEX = Regex("^/([a-zA-Z0-9])+")
private val TG_COMMAND_NO_SLASH_REGEX = Regex("""\b([a-zA-Z0-9_]+)\b""")

/**
 * Escapes Markdown v2 special characters in a string. Useful for sending formatted messages.
 * @return escaped version of the string.
 */
fun String.escapeMarkdownV2(): String {
    val escapedChars = "_*[]()~`>#+-=|{}.!".toSet()
    return this.map { char ->
        if (char in escapedChars) "\\$char" else "$char"
    }.joinToString("")
}

fun String.isCommand(withSlash: Boolean = true): Boolean {
    return if (withSlash) {
        TG_COMMAND_REGEX.matches(this)
    } else {
        TG_COMMAND_NO_SLASH_REGEX.matches(this)
    }
}

/**
 * Breaks down a string into a list of strings (command name, args).
 * The spacing in the trailing unlimited-length argument is unaltered
 *
 * Example: `breakdownCommand("/command arg1 arg2  arg3", 1) -> ["/command", "arg1", "arg2  arg3"]`
 *
 * @param maxArgs Maximum count of args before the trailing unlimited-length argument
 * @return The list containing the command and its arguments, with the first argument being the command
 */
fun String.breakdownCommand(maxArgs: Int? = null): List<String> {
    val s = trim()
    if (s.isEmpty()) return emptyList()

    // No maxArgs: normal whitespace split
    if (maxArgs == null) {
        return s.split(Regex("\\s+"))
    }

    val result = mutableListOf<String>()
    var remaining = s

    // Extract command name
    val firstSplit = remaining.indexOf(' ')
    if (firstSplit < 0) {
        // No arguments at all
        return listOf(remaining)
    }

    result += remaining.substring(0, firstSplit)
    remaining = remaining.substring(firstSplit).trimStart()

    // Extract up to maxArgs arguments
    repeat(maxArgs) {
        val splitIndex = remaining.indexOf(' ')
        if (splitIndex < 0) {
            // No more spaces → this is the last argument
            result += remaining
            return result
        }
        val arg = remaining.substring(0, splitIndex)
        result += arg
        remaining = remaining.substring(splitIndex).trimStart()
    }

    // The rest is the final unlimited-length argument (preserve whitespace)
    if (remaining.isNotEmpty()) {
        result += remaining
    }

    return result
}

/**
 * Computes the MD5 hash (128-bit, 32 hexadecimal characters) of the string.
 */
fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray()))
        .toString(16)
        .padStart(32, '0')
}

fun formatSnippetForMarkdownV2(snippet: String): String {
    return snippet
        .lines()
        .filter { it.isNotBlank() }
        .joinToString("\n") { line ->
            "> " + line
                .trim()
                .escapeMarkdownV2()
                .replace("<b\\>", "*")
                .replace("</b\\>", "*")
        }
}

fun localizeRole(userRole: UserRole): String {
    return when (userRole) {
        UserRole.GUEST -> "гость"
        UserRole.REGULAR -> "зарегистрированный пользователь"
        UserRole.ADMIN -> "администратор"
    }
}
