package io.github.colintimbarndt.chat_emotes.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult

interface MatchTransformer {
    fun transform(r: MatchResult, result: StringBuilder)

    fun acceptsWidth(w: Int): Boolean = w == acceptedWidth

    val acceptedWidth: Int

    val requiresName: Boolean
        get() = false
}