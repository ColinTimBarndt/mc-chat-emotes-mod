package io.github.colintimbarndt.chat_emotes.data.unicode.joiner;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import org.jetbrains.annotations.NotNull;

public interface MatchTransformer {
    void transform(@NotNull MatchResult r, @NotNull StringBuilder result);
    default @NotNull String transform(@NotNull MatchResult r) {
        final var builder = new StringBuilder();
        transform(r, builder);
        return builder.toString();
    }
    default boolean acceptsWidth(int w) {
        return w == acceptedWidth();
    }
    int acceptedWidth();
    default boolean requiresName() {
        return false;
    }
}
