package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class PatternSlot implements Matcher {
    private final @NotNull Matcher @NotNull[] matchers;
    private final int width;
    public final @NotNull SlotRange range;
    public final @NotNull String delimiter;
    public final boolean captured;
    private final boolean named;

    public PatternSlot(
            @NotNull Matcher @NotNull[] matchers,
            @NotNull SlotRange range,
            @NotNull String delimiter,
            boolean captured
    ) {
        this.matchers = matchers;
        this.range = range;
        this.delimiter = delimiter;
        int width = 0;
        for (var m : matchers) width += m.width();
        this.width = width;
        this.captured = captured;
        this.named = Arrays.stream(matchers).allMatch(Matcher::hasName);
    }

    @Override
    public MatchResult matchWith(String s, int offset) {
        int idxOffset = 0;
        for (var m : matchers) {
            final var r = m.matchWith(s, offset);
            if (r != null) return r.withOffset(idxOffset);
            idxOffset += m.width();
        }
        return null;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public boolean hasName() {
        return named;
    }
}
