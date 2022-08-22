@file:JvmName("EmoteDecoratorTestKt")

package io.github.colintimbarndt.chat_emotes.common

import io.github.colintimbarndt.chat_emotes.common.MockEmoteDataLoader.BAR_BAZ_EMOTE
import io.github.colintimbarndt.chat_emotes.common.MockEmoteDataLoader.BAR_EMOTE
import io.github.colintimbarndt.chat_emotes.common.MockEmoteDataLoader.BAZ_EMOTE
import io.github.colintimbarndt.chat_emotes.common.MockEmoteDataLoader.FOO_EMOTE
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EmoteDecoratorTest {
    @Test
    fun comboTest() {
        // result:      foo  bar  bar:baz   foo  bar:baz
        val text = "ABC:foo::bar::bar::baz::baz::bar::baz:DEF"
        // start:       ^  :    :    :    :    :    :    :
        // ends:           ^    ^    ^    ^    ^    ^    ^
        val start = text.indexOf(':') + 1
        val ends = IntArrayList.of(
            start + 3,
            start + 3 + 5,
            start + 3 + 5 * 2,
            start + 3 + 5 * 3,
            start + 3 + 5 * 4,
            start + 3 + 5 * 5,
            start + 3 + 5 * 6,
        )
        val result = listOf(
            "foo" to FOO_EMOTE,
            "bar" to BAR_EMOTE,
            "bar::baz" to BAR_BAZ_EMOTE,
            "baz" to BAZ_EMOTE,
            "bar::baz" to BAR_BAZ_EMOTE,
        )

        var i = 0
        MockServerMod.emoteDecorator.emotesForAliasCombo(
            text,
            ends,
            start,
        ) { alias, emote, _, _ ->
            val (expectAlias, expectEmote) = result[i]
            assertEquals(expectAlias, alias)
            assertEquals(expectEmote, emote)
            i++
        }
    }
}