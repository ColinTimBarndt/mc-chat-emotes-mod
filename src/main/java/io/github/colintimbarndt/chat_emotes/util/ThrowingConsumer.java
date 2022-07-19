package io.github.colintimbarndt.chat_emotes.util;

import java.io.IOException;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T value) throws E;
}
