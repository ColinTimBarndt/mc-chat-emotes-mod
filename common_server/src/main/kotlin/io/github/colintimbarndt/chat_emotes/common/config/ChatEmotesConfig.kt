package io.github.colintimbarndt.chat_emotes.common.config

import com.google.gson.*
import com.google.gson.stream.JsonWriter
import io.github.colintimbarndt.chat_emotes.common.config.ResourcepackConfig.ListSerializer
import io.github.colintimbarndt.chat_emotes.common.util.BomAwareReader
import net.minecraft.util.GsonHelper
import java.io.*
import java.lang.reflect.Type

class ChatEmotesConfig private constructor(val resourcepacks: List<ResourcepackConfig>) {

    constructor() : this(emptyList<ResourcepackConfig>())

    @Throws(IOException::class)
    fun save(output: OutputStream) {
        JsonWriter(OutputStreamWriter(output)).use { writer -> GSON.toJson(this, ChatEmotesConfig::class.java, writer) }
    }

    object Serializer : JsonSerializer<ChatEmotesConfig>, JsonDeserializer<ChatEmotesConfig> {
        override fun serialize(config: ChatEmotesConfig, type: Type, ctx: JsonSerializationContext): JsonElement {
            val root = JsonObject()
            root.add("resourcepacks", ctx.serialize(config.resourcepacks, ResourcepackConfig.LIST_TYPE))
            return root
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            element: JsonElement,
            type: Type,
            ctx: JsonDeserializationContext
        ): ChatEmotesConfig {
            val root = element.asJsonObject
            val resourcepacks = ctx.deserialize<List<ResourcepackConfig>>(
                GsonHelper.getAsJsonObject(root, "resourcepacks"),
                ResourcepackConfig.LIST_TYPE
            )
            return ChatEmotesConfig(resourcepacks)
        }
    }

    companion object {
        private val GSON = GsonBuilder()
            .registerTypeAdapter(ResourcepackConfig.LIST_TYPE, ListSerializer)
            .registerTypeAdapter(ChatEmotesConfig::class.java, Serializer)
            .create()

        @Throws(IOException::class, JsonParseException::class)
        fun load(input: InputStream?): ChatEmotesConfig {
            BomAwareReader.create(input).use { reader ->
                return GSON.fromJson(reader, ChatEmotesConfig::class.java)
            }
        }
    }
}