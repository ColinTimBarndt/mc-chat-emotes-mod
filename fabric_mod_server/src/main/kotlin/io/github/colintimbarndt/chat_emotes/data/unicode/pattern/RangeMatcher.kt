package io.github.colintimbarndt.chat_emotes.data.unicode.pattern

/**
 * Matches all characters in the inclusive range [fromCodePoint]..[toCodePoint]. The resulting index may be [reversed]
 * @see NamedRangeMatcher
 */
data class RangeMatcher(val fromCodePoint: Int, val toCodePoint: Int, val reversed: Boolean = false) : Matcher {

    override fun matchWith(s: String, offset: Int): MatchResult? {
        if (offset >= s.length) return null
        val cp = s.codePointAt(offset)
        return if (cp in fromCodePoint..toCodePoint) MatchResult(
            Character.toString(cp),
            if (reversed) toCodePoint - cp + fromCodePoint else cp - fromCodePoint
        ) else null
    }

    override val width = 1 + toCodePoint - fromCodePoint
}