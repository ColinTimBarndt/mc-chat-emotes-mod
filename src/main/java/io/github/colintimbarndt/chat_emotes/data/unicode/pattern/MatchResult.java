package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MatchResult(
        @NotNull String value,
        int index,
        @Nullable String name
) {
    public MatchResult(@NotNull String value, int index) {
        this(value, index, null);
    }
    @Contract("_ -> new")
    public @NotNull MatchResult withOffset(int offset) {
        return new MatchResult(value, index + offset, name);
    }

    @Contract("_ -> new")
    public @NotNull MatchResult withIndex(int index) {
        return new MatchResult(value, index, name);
    }
}
