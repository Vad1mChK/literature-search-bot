package com.vad1mchk.litsearchbot.command

import com.vad1mchk.litsearchbot.auth.UserRole
import com.vad1mchk.litsearchbot.commands.BotCommand
import org.junit.jupiter.api.Test

class BotCommandTest {
    val guestCommand = BotCommand("help", "", UserRole.GUEST, {})
    val regularCommand = BotCommand("search", "", UserRole.REGULAR, {})
    val adminCommand = BotCommand("a_reindex", "", UserRole.ADMIN, {})

    @Test
    fun botCommand_forMinRoleOrHigher_shouldPermitAccess() {
        assert(guestCommand.permitsRole(UserRole.GUEST))
        assert(guestCommand.permitsRole(UserRole.REGULAR))
        assert(guestCommand.permitsRole(UserRole.ADMIN))
        assert(regularCommand.permitsRole(UserRole.REGULAR))
        assert(regularCommand.permitsRole(UserRole.ADMIN))
        assert(adminCommand.permitsRole(UserRole.ADMIN))
    }

    @Test
    fun botCommand_forLowerThanMinRole_shouldNotPermitAccess() {
        assert(!regularCommand.permitsRole(UserRole.GUEST))
        assert(!adminCommand.permitsRole(UserRole.GUEST))
        assert(!adminCommand.permitsRole(UserRole.REGULAR))
    }
}
