package io.github.colintimbarndt.chat_emotes.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult

class EnumeratedTransformer(private val transformers: Array<MatchTransformer>) : MatchTransformer {
    override val acceptedWidth: Int
    private val linear: Boolean

    init {
        var aw = 0
        var linear0 = true
        for (t in transformers) {
            aw += t.acceptedWidth
            linear0 = linear0 && t.acceptedWidth == 1
        }
        linear = linear0
        acceptedWidth = aw
    }

    override fun transform(r: MatchResult, result: StringBuilder) {
        if (linear) {
            // Optimization
            transformers[r.index].transform(r, result)
            return
        }
        var index = r.index
        for (t in transformers) {
            val aw = t.acceptedWidth
            if (index < aw) {
                t.transform(r.withIndex(index), result)
                return
            }
            index -= aw
        }
    }
}