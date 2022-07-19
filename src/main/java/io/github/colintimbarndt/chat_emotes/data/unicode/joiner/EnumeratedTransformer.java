package io.github.colintimbarndt.chat_emotes.data.unicode.joiner;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import org.jetbrains.annotations.NotNull;

public final class EnumeratedTransformer implements MatchTransformer {
    private final MatchTransformer[] transformers;
    private final int acceptedWidth;
    private boolean linear = true;

    public EnumeratedTransformer(MatchTransformer[] transformers) {
        this.transformers = transformers;
        int aw = 0;
        for (var t : transformers) {
            aw += t.acceptedWidth();
            if (aw != 1) linear = false;
        }
        acceptedWidth = aw;
    }

    @Override
    public void transform(@NotNull MatchResult r, @NotNull StringBuilder result) {
        if (linear) {
            // Optimization
            transformers[r.index()].transform(r, result);
            return;
        }
        int index = r.index();
        for (var t : transformers) {
            final int aw = t.acceptedWidth();
            if (index < aw) {
                t.transform(r.withIndex(index), result);
                return;
            }
            index -= aw;
        }
    }

    @Override
    public int acceptedWidth() {
        return acceptedWidth;
    }
}
