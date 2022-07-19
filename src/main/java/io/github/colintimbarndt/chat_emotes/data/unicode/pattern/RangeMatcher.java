package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RangeMatcher(int fromCodePoint, int toCodePoint, boolean reversed) implements Matcher {
    public RangeMatcher(int fromCodePoint, int toCodePoint) {
        this(fromCodePoint, toCodePoint, false);
    }

    @Override
    public @Nullable MatchResult matchWith(@NotNull String s, int offset) {
        if (offset >= s.length()) return null;
        final int cp = s.codePointAt(offset);
        return cp >= fromCodePoint && cp <= toCodePoint
                ? new MatchResult(
                        Character.toString(cp),
                        reversed ? toCodePoint - cp + fromCodePoint : cp - fromCodePoint)
                : null;
    }

    @Override
    public int width() {
        return 1 + toCodePoint - fromCodePoint;
    }
}
