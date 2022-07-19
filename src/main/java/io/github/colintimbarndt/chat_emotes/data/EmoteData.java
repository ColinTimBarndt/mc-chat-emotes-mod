package io.github.colintimbarndt.chat_emotes.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public interface EmoteData {
    @NotNull ResourceLocation getLocation();
    @NotNull Set<Emote> getEmotes();

    /**
     * @return {@link Set} of all aliases this data covers
     */
    @NotNull Set<String> getAliases();

    /**
     * @return {@link Set} of all emoticons this data covers
     */
    @NotNull Set<String> getEmoticons();

    @Nullable Emote emoteForUnicodeSequence(@NotNull String sequence);

    @Nullable Emote emoteForAlias(@NotNull String alias);

    @Nullable Emote emoteForEmoticon(@NotNull String emoticon);

    @NotNull EmoteDataSerializer<?> getSerializer();

    void generateFonts(@NotNull FontGenerator gen, @NotNull Path imageSources) throws IOException;
}
