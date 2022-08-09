package io.github.colintimbarndt.chat_emotes.data

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import net.minecraft.resources.ResourceLocation

interface EmoteDataSerializer<T : EmoteData> {
    /**
     * Reads [EmoteData] from a JSON file that is provided by a data pack using the given [location] and optionally
     * provided [samples]
     */
    @Throws(JsonParseException::class)
    fun read(
        location: ResourceLocation,
        json: JsonObject,
        samples: Set<String>?
    ): T
}