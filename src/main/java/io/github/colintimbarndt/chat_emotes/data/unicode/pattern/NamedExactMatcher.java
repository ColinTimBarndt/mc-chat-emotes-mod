package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public record NamedExactMatcher(String name) implements NamedMatcher<ExactMatcher> {
    @Override
    public void getNameReferences(@NotNull Consumer<String> consumer) {
        consumer.accept(name);
    }

    @Contract("_ -> new")
    @Override
    public @NotNull ExactMatcher resolveNames(@NotNull Function<String, String> resolver) {
        return new ExactMatcher(Objects.requireNonNull(resolver.apply(name)), name);
    }
}
