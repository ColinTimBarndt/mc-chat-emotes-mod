package io.github.colintimbarndt.chat_emotes;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class TranslationHelper {

    public static MutableComponent fallback(Component fallback, Component other) {
        return Component.translatable("%1$s%784014$s", fallback, other);
    }

    public static MutableComponent fallbackTranslatable(String key, Object ...args) {
        final var inner = Component.translatable(key, args);
        final var builder = new StringBuilder();
        inner.visit((s) -> {
            builder.append(s);
            return Optional.empty();
        });
        return fallback(Component.literal(builder.toString()), inner);
    }

}
