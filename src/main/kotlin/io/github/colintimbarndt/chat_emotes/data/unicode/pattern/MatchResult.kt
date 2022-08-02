package io.github.colintimbarndt.chat_emotes.data.unicode.pattern

/**
 * Represents the result of a [Matcher]
 */
data class MatchResult(
    /**
     * The sequence of characters that was matched
     */
    val value: String,
    /**
     * The unique index of the matched sequence
     * @see Matcher
     */
    val index: Int,
    /**
     * A name that was assigned to the matched sequence by the [Matcher], which is optional
     */
    val name: String? = null
) {
    /**
     * @return New [MatchResult] with [offset] added to the [index]
     */
    infix fun offsetBy(offset: Int): MatchResult {
        return MatchResult(value, index + offset, name)
    }

    /**
     * @return New [MatchResult] with the given index
     */
    infix fun withIndex(index: Int): MatchResult {
        return MatchResult(value, index, name)
    }
}