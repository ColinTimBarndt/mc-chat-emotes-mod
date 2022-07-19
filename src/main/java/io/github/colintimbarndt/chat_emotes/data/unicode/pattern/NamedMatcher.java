package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import java.util.function.Consumer;
import java.util.function.Function;

public interface NamedMatcher<T extends Matcher> {
    void getNameReferences(Consumer<String> consumer);
    T resolveNames(Function<String, String> resolver);
}
