package io.github.colintimbarndt.chat_emotes.data.unicode.joiner;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class IdentityTransformer implements MatchTransformer {
    public static final IdentityTransformer INSTANCE = new IdentityTransformer();
    private IdentityTransformer() {}
    @Override
    public void transform(@NotNull MatchResult r, @NotNull StringBuilder result) {
        result.append(Objects.requireNonNull(r.name()));
    }

    @Override
    public boolean acceptsWidth(int w) {
        return true;
    }

    @Override
    public int acceptedWidth() {
        return 0;
    }

    @Override
    public boolean requiresName() {
        return true;
    }
}
