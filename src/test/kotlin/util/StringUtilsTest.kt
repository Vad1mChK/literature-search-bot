package com.vad1mchk.litsearchbot.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StringUtilsTest {
    @Test
    fun testMarkdownEscape() {
        val testCases =
            mapOf(
                "" to "",
                "> be me\n> programmer" to "\\> be me\n\\> programmer",
                "`./gradlew run`" to """\`\./gradlew run\`""",
                "![Image](https://example.com/image.jpg)" to """\!\[Image\]\(https://example\.com/image\.jpg\)""",
            )
        for (testString in testCases.keys) {
            val expected = testCases[testString]
            val actual = testString.escapeMarkdownV2()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun testBreakdownCommand() {
        assertEquals("".breakdownCommand(), emptyList())
        assertEquals(" \n\t ".breakdownCommand(), emptyList())
        assertEquals("/command".breakdownCommand(), listOf("/command"))
        assertEquals("/help command".breakdownCommand(), listOf("/help", "command"))
        assertEquals(
            "/command arg0 arg1 arg2  ".breakdownCommand(),
            listOf("/command", "arg0", "arg1", "arg2"),
        )
        assertEquals(
            "/command arg0 arg1 arg2 this should not  be separated".breakdownCommand(maxArgs = 3),
            listOf("/command", "arg0", "arg1", "arg2", "this should not  be separated"),
        )
        assertEquals(
            "/command arg0 arg1 arg2 this should not  be separated".breakdownCommand(maxArgs = 0),
            listOf("/command", "arg0 arg1 arg2 this should not  be separated"),
        )
    }

    @Test
    fun testMD5Hash() {
        val md5Regex = Regex("""^[0-9A-Fa-f]{32}$""")
        val testStrings = listOf(
            "",
            " ",
            "venom",
            "/home/v/Documents/literature/file.pdf",
            "C:\\Users\\V\\Documents\\Literature\\file.pdf",
        )
        for (testString in testStrings) {
            val result = md5(testString)
            assertEquals(result.length, 32)
            assert(result.matches(md5Regex))
        }
    }
}
