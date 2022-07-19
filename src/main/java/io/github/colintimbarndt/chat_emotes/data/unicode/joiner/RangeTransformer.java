package io.github.colintimbarndt.chat_emotes.data.unicode.joiner;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import org.jetbrains.annotations.NotNull;

public final class RangeTransformer implements MatchTransformer {
    private final int base;
    private final int width;
    private final boolean reversed;

    public RangeTransformer(int from, int to) {
        if (from <= to) {
            this.base = from;
            this.width = 1 + to - from;
            this.reversed = false;
        } else {
            this.base = to;
            this.width = 1 + from - to;
            this.reversed = true;
        }
    }

    @Override
    public void transform(@NotNull MatchResult r, @NotNull StringBuilder result) {
        final var ch = reversed ? base - r.index() : base + r.index();
        if (Character.isBmpCodePoint(ch)) {
            result.append((char) ch);
        } else {
            result.append(Character.highSurrogate(ch))
                    .append(Character.lowSurrogate(ch));
        }
    }

    @Override
    public int acceptedWidth() {
        return width;
    }
}
