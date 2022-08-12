package io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern

/**
 * @see NamedPatternSlot
 */
data class PatternSlot(
    private val matchers: Array<Matcher>,
    override val range: SlotQuantifier,
    override val delimiter: String,
    override val captured: Boolean
) : IPatternSlot, Matcher {

    override val width: Int = matchers.asSequence().sumOf(Matcher::width)

    override val hasName: Boolean = matchers.asSequence().all(Matcher::hasName)

    override fun matchWith(s: String, offset: Int): MatchResult? {
        var idxOffset = 0
        for (m in matchers) {
            val r = m.matchWith(s, offset)
            if (r != null) return r.offsetBy(idxOffset)
            idxOffset += m.width
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatternSlot

        return equalsHelper(PatternSlot::matchers, other)
    }

    override fun hashCode(): Int {
        var result = matchers.contentHashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + delimiter.hashCode()
        result = 31 * result + captured.hashCode()
        result = 31 * result + width
        result = 31 * result + hasName.hashCode()
        return result
    }
}