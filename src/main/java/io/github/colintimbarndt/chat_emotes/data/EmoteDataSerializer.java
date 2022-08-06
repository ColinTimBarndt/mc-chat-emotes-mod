package io.github.colintimbarndt.chat_emotes.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface EmoteDataSerializer<T extends EmoteData> {
    /**
     * Reads the defined emotes from a JSON configuration
     * @param location identifier of this emote data
     * @param json raw configuration
     * @param samples optional mutable set of sample values
     * @return deserialized emote data
     * @throws JsonParseException when the given configuration contains errors
     */
    @NotNull T read(
            @NotNull ResourceLocation location,
            @NotNull JsonObject json,
            @Nullable Set<String> samples
    ) throws JsonParseException;
}
