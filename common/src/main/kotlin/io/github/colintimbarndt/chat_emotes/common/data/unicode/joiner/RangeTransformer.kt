package io.github.colintimbarndt.chat_emotes.common.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern.MatchResult

class RangeTransformer(from: Int, to: Int) : MatchTransformer {
    private var base = 0
    override val acceptedWidth: Int
    private var reversed = false

    init {
        if (from <= to) {
            base = from
            acceptedWidth = 1 + to - from
            reversed = false
        } else {
            base = to
            acceptedWidth = 1 + from - to
            reversed = true
        }
    }

    override fun transform(r: MatchResult, result: StringBuilder) {
        val ch = if (reversed) base - r.index else base + r.index
        result.appendCodePoint(ch)
    }
}