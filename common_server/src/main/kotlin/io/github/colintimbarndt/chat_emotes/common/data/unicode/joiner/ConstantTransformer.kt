package io.github.colintimbarndt.chat_emotes.common.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern.MatchResult
import io.github.colintimbarndt.chat_emotes.common.util.StringBuilderExt.plusAssign

data class ConstantTransformer(val value: String) : MatchTransformer {
    override fun transform(r: MatchResult, result: StringBuilder) {
        result += value
    }

    override val acceptedWidth = 1
}