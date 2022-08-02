package io.github.colintimbarndt.chat_emotes.data.unicode;

import org.jetbrains.annotations.NotNull;

public final class UnicodeUtil {
    private UnicodeUtil() {}
    public static final char ZWNJ = '\u200C';
    public static final char ZWJ = '\u200D';
    public static final char VS15 = '\uFE0E';
    /**
     * An invisible codepoint which specifies that the preceding character should be displayed with emoji presentation.
     * Only required if the preceding character defaults to text presentation.
     * @see <a href="https://unicode-table.com/en/FE0F/">Unicode Table</a>
     */
    public static final char VS16 = '\uFE0F';
    /**
     * Ends a tag sequence
     * @see <a href="https://unicode-table.com/en/E007F/">Unicode Table</a>
     */
    public static final String CANCEL = "\udb40\udc7f";

    /**
     * Creates a tag sequence
     * @param s content of the tag
     * @return unicode tag sequence
     */
    public static @NotNull String tagged(final @NotNull String s) {
        final StringBuilder builder = new StringBuilder(2 * s.length());
        s.codePoints().forEachOrdered(cp -> {
            if (cp >= 0x7F)
                throw new IllegalArgumentException("Illegal characters in string");
            cp += 0xE0000;
            builder.append(Character.highSurrogate(cp))
                    .append(Character.lowSurrogate(cp));
        });
        builder.append(CANCEL);
        return builder.toString();
    }
}
