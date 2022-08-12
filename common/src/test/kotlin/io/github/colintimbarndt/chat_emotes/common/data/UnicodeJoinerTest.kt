package io.github.colintimbarndt.chat_emotes.common.data

import io.github.colintimbarndt.chat_emotes.common.data.unicode.joiner.UnicodeJoiner
import io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern.UnicodePattern
import io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern.UnicodePattern.Companion.parse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class UnicodeJoinerTest {
    @Test
    fun transformFlags() = joinerTest("{regional_indicator_a-regional_indicator_z#2}") {
        positive(
            arrayOf(
                // 🇨🇦
                "\uD83C\uDDE8\uD83C\uDDE6" to "flag_ca",
                // 🇫🇮
                "\uD83C\uDDEB\uD83C\uDDEE" to "flag_fi",
                // 🇿🇦
                "\uD83C\uDDFF\uD83C\uDDE6" to "flag_za",
                // 🇦🇿
                "\uD83C\uDDE6\uD83C\uDDFF" to "flag_az",
            ),
            "flag_{a-z#2}",
            "flag_{a-z}{a-z}",
            "flag_{a-v|w|x-z#2}",
        )
        negative("{a-y#2}")
        negative("{a-z}")
        negative("{a-z#3}")
    }

    @Test
    fun transformFamily() = joinerTest("{man|woman#1-2&+}+{boy|girl#1-2&+}") {
        // 👨‍👩‍👧‍👦
        positive(
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66",
            "family_{m|w#?}{b|g#?}" to "family_mwgb",
            "family_{#*&_}" to "family_man_woman_girl_boy",
        )
        // 👨‍👩‍👦
        positive(
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66",
            "family_{m|w#?}{b|g#?}" to "family_mwb",
            "family_{#*&_}" to "family_man_woman_boy",
        )
        negative("family_{#2&_}")
    }

    private fun joinerTest(
        patternStr: String,
        inputs: TestInput<UnicodePattern>.() -> Unit
    ) {
        val pattern = assertDoesNotThrow { parse(patternStr) }
        pattern.resolveNames { EMOTES[it]!! }
        inputs(TestInput { it(pattern) })
    }

    private fun TestInput<UnicodePattern>.positive(input: String, vararg pairs: Pair<String, String>) = accept {
        val matches = it.apply(input)!!
        for((joinerStr, expected) in pairs) {
            val joiner = assertDoesNotThrow { UnicodeJoiner.parse(joinerStr) }
            assertTrue(joiner.isCompatibleWith(it))
            val joined = joiner.evaluate(matches)
            assertEquals(expected, joined, "positive input $input with $joinerStr")
        }
    }

    private fun TestInput<UnicodePattern>.positive(pairs: Array<Pair<String, String>>, vararg joinerStrs: String) {
        for((input, expected) in pairs) {
            val pairs0 = joinerStrs.map { it to expected }
            positive(input, *pairs0.toTypedArray())
        }
    }

    private fun TestInput<UnicodePattern>.negative(joinerStr: String) = accept {
        val joiner = assertDoesNotThrow { UnicodeJoiner.parse(joinerStr) }
        assertFalse(joiner.isCompatibleWith(it), "negative test: $joinerStr")
    }
}
