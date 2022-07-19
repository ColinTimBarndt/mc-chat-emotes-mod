package io.github.colintimbarndt.chat_emotes.data.unicode.joiner;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import org.jetbrains.annotations.NotNull;

public record ConstantTransformer(String value) implements MatchTransformer {
    @Override
    public void transform(@NotNull MatchResult r, @NotNull StringBuilder result) {
        result.append(value);
    }

    @Override
    public int acceptedWidth() {
        return 1;
    }
}
