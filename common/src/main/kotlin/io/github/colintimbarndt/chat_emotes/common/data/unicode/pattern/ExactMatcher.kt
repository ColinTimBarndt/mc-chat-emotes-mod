package io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern

/**
 * Matches [text] exactly
 * @see NamedExactMatcher
 */
data class ExactMatcher(val text: String, val name: String) : Matcher {
    override fun matchWith(s: String, offset: Int): MatchResult? {
        return if (s.startsWith(text, offset)) MatchResult(text, 0, name) else null
    }

    override val width = 1

    override val hasName = true
}