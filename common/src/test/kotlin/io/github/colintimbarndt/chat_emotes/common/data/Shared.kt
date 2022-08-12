package io.github.colintimbarndt.chat_emotes.common.data

import io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern.MatchResult

val EMOTES = mapOf(
    "flag_white" to Character.toString(0x1F3F3),
    "flag_black" to Character.toString(0x1F3F4),
    "rainbow" to Character.toString(0x1f308),
    "boy" to Character.toString(0x1f466),
    "girl" to Character.toString(0x1f467),
    "man" to Character.toString(0x1f468),
    "woman" to Character.toString(0x1f469),
    "regional_indicator_a" to Character.toString(0x1F1E6),
    "regional_indicator_z" to Character.toString(0x1F1FF),
)

data class TestInput<T>(
    inline val accept: ((T) -> Unit) -> Unit
)

typealias MatchCaptures = Array<List<MatchResult>>
