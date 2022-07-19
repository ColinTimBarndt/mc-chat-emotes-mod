package io.github.colintimbarndt.chat_emotes.util;

import org.jetbrains.annotations.NotNull;

public final class ArrayUtils {
    private static final Object[] EMPTY_ARRAY = {};

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T[] emptyArray() {
        return (T[])EMPTY_ARRAY;
    }
}
