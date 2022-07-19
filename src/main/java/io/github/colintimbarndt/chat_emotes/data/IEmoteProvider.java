package io.github.colintimbarndt.chat_emotes.data;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2CharMap;
import org.jetbrains.annotations.NotNull;

public interface IEmoteProvider {
    @NotNull Object2CharMap<String> getCharMappings();
    @NotNull Char2ObjectMap<String[]> getAliases();
    @NotNull Object2CharMap<String> getAliasesInverse();
    @NotNull Char2ObjectMap<String[]> getEmoticons();
    @NotNull Object2CharMap<String> getEmoticonsInverse();
}
