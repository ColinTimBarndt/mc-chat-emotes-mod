package io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern

/**
 * The abstract idea of a "Matcher" is that it may or may not match against a series of characters where the total
 * set of possible matches is finite and countable such that every distinct matched sequence has a unique
 * [MatchResult.index]
 * @see NamedMatcher
 */
interface Matcher {
    /**
     * Attempts to match this matcher with a string
     * @param s String to match against
     * @param offset At witch position to match
     * @return Index of the match or `null` otherwise
     */
    fun matchWith(s: String, offset: Int): MatchResult?

    /**
     * The amount of patterns that can be matched
     */
    val width: Int

    /**
     * whether this matcher will return named [MatchResult]s
     */
    val hasName: Boolean
        get() = false
}