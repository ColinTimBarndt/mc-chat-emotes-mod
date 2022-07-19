package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.Nullable;

public interface Matcher {
    /**
     * Attempts to match this matcher with a string
     * @param s String to match against
     * @param offset At witch position to match
     * @return Index of the match or <code>-1</code> otherwise
     */
    @Nullable MatchResult matchWith(String s, int offset);

    /**
     * @return The amount of patterns that can be matched
     */
    int width();

    /**
     * @return whether this matcher will return named {@link MatchResult}s
     */
    default boolean hasName() {
        return false;
    }
}
