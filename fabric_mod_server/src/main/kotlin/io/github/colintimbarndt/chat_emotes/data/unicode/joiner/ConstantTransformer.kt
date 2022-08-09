package io.github.colintimbarndt.chat_emotes.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult
import io.github.colintimbarndt.chat_emotes.util.StringBuilderExt.plusAssign

data class ConstantTransformer(val value: String) : MatchTransformer {
    override fun transform(r: MatchResult, result: StringBuilder) {
        result += value
    }

    override val acceptedWidth = 1
}