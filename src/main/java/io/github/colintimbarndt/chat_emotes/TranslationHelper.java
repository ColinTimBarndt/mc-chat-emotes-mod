package io.github.colintimbarndt.chat_emotes;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Optional;

public class TranslationHelper {

    public static MutableText fallback(Text fallback, Text other) {
        return Text.translatable("%1$s%784014$s", fallback, other);
    }

    public static MutableText translatable(String key, Object ...args) {
        final var inner = Text.translatable(key, args);
        return inner;
        //final var builder = new StringBuilder();
        //inner.visit((s) -> {
        //    builder.append(s);
        //    return Optional.empty();
        //});
        //return fallback(Text.of(builder.toString()), inner);
    }

}
