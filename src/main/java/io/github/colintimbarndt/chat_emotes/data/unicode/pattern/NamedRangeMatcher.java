package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import java.util.function.Consumer;
import java.util.function.Function;

public record NamedRangeMatcher(String fromName, String toName) implements NamedMatcher<RangeMatcher> {
    @Override
    public void getNameReferences(Consumer<String> consumer) {
        consumer.accept(fromName);
        consumer.accept(toName);
    }

    @Override
    public RangeMatcher resolveNames(Function<String, String> resolver) {
        final int from = toCodePoint(resolver.apply(fromName));
        final int to = toCodePoint(resolver.apply(toName));
        return from <= to ? new RangeMatcher(from, to) : new RangeMatcher(to, from, true);
    }

    private static int toCodePoint(String s) {
        return s.length() > 0 ? s.codePointAt(0) : 0;
    }
}
