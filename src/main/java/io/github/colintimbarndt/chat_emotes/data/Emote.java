package io.github.colintimbarndt.chat_emotes.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Emote(
        @NotNull ResourceLocation font,
        char character,
        @NotNull String @NotNull[] aliases,
        @NotNull String @NotNull[] emoticons,
        @Nullable String unicodeSequence
) {
}
