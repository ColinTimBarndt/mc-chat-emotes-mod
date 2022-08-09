package io.github.colintimbarndt.chat_emotes.data

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern.Companion.parse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertDoesNotThrow

class UnicodePatternTest {
    // 🏴󠁧󠁢󠁥󠁮󠁧󠁿
    @Test
    fun parseGbEngFlag() = parseTest("{flag_black}T(gbeng)") {
        positive(
            "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F",
            arrayOf(listOf(MatchResult("\ud83c\udff4", 0, "flag_black")))
        )
        negative("")
        // 👨‍👩‍👧
        negative("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67")
        // 🏳️‍🌈
        negative("\uD83C\uDFF3️\u200D\uD83C\uDF08")
        // Broken flags
        negative("\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67")
        negative("\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC7F")
    }

    // 🏳️‍🌈
    @Test
    fun parseRainbowFlag() = parseTest("{flag_white}E+{rainbow}") {
        positive(
            "\uD83C\uDFF3️\u200D\uD83C\uDF08",
            arrayOf(
                listOf(MatchResult("\ud83c\udff3", 0, "flag_white")),
                listOf(MatchResult("\uD83C\uDF08", 0, "rainbow"))
            )
        )
        negative("")
        // 👨‍👩‍👧 (man, woman, girl)
        negative("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67")
        // 🏴️‍🌈 (black flag, rainbow)
        negative("\uD83C\uDFF4️\u200D\uD83C\uDF08")
    }

    @Test
    fun parseFamily() = parseTest("{man|woman#1-2&+}+{boy|girl#1-2&+}") {
        // 👨‍👩‍👧‍👦 (man, woman, girl, boy)
        positive(
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66",
            arrayOf(
                listOf(
                    MatchResult("\uD83D\uDC68", 0, "man"),
                    MatchResult("\uD83D\uDC69", 1, "woman"),
                ),
                listOf(
                    MatchResult("\uD83D\uDC67", 1, "girl"),
                    MatchResult("\uD83D\uDC66", 0, "boy"),
                )
            )
        )
        // 👨‍👩‍👧
        positive(
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67",
            arrayOf(
                listOf(
                    MatchResult("\uD83D\uDC68", 0, "man"),
                    MatchResult("\uD83D\uDC69", 1, "woman"),
                ),
                listOf(
                    MatchResult("\uD83D\uDC67", 1, "girl"),
                )
            )
        )
        // 👨‍👧
        positive(
            "\uD83D\uDC68\u200D\uD83D\uDC67",
            arrayOf(
                listOf(MatchResult("\uD83D\uDC68", 0, "man")),
                listOf(MatchResult("\uD83D\uDC67", 1, "girl"))
            )
        )
        // 👩‍👧‍👦
        positive(
            "\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66",
            arrayOf(
                listOf(MatchResult("\uD83D\uDC69", 1, "woman")),
                listOf(
                    MatchResult("\uD83D\uDC67", 1, "girl"),
                    MatchResult("\uD83D\uDC66", 0, "boy")
                )
            )
        )
        negative("")
        // 🏳️‍🌈
        negative("\uD83C\uDFF3️\u200D\uD83C\uDF08")
        // 👨‍👩 (man, woman)
        negative("\uD83D\uDC68\u200D\uD83D\uDC69")
    }

    @Test
    fun parseNoCapture() = parseTest("{!flag_black}{flag_white}{!flag_black}{man}") {
        // 🏴🏳🏴👨
        positive(
            "\uD83C\uDFF4\uD83C\uDFF3\uD83C\uDFF4\uD83D\uDC68",
            arrayOf(
                listOf(MatchResult("\uD83C\uDFF3", 0, "flag_white")),
                listOf(MatchResult("\uD83D\uDC68", 0, "man"))
            )
        )
    }

    @Test
    fun parseLiteral() = parseTest("L{!don't }L{panic\\!}") {
        positive(
            "don't panic!",
            arrayOf(listOf(MatchResult("panic!", 0, "panic!")))
        )
    }

    private fun parseTest(
        patternString: String,
        inputs: TestInput<UnicodePattern>.() -> Unit
    ) {
        val pattern = assertDoesNotThrow { parse(patternString) }
        pattern.resolveNames { EMOTES[it]!! }
        inputs(TestInput { it(pattern) })
    }

    private fun TestInput<UnicodePattern>.positive(input: String, expected: MatchCaptures?) = accept {
        assertArrayEquals(expected, it.apply(input), "positive input $input")
    }

    private fun TestInput<UnicodePattern>.negative(input: String) = accept {
        assertNull(it.apply(input), "negative input $input")
    }
}