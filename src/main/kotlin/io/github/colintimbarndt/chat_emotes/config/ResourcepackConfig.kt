package io.github.colintimbarndt.chat_emotes.config

import com.google.common.reflect.TypeToken
import com.google.gson.*
import com.mojang.bridge.game.PackType
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod.Companion.MOD_ID
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod.Companion.configDir
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod.Companion.modMetadata
import io.github.colintimbarndt.chat_emotes.data.EmoteData
import io.github.colintimbarndt.chat_emotes.data.FontGenerator
import io.github.colintimbarndt.chat_emotes.data.PackExportException
import io.github.colintimbarndt.chat_emotes.util.PackWriter
import io.github.colintimbarndt.chat_emotes.util.PackWriter.PackMeta
import net.minecraft.ChatFormatting
import net.minecraft.ResourceLocationException
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import org.jetbrains.annotations.UnmodifiableView
import java.io.*
import java.lang.reflect.Type
import java.nio.file.Path
import java.util.*

class ResourcepackConfig(val name: String, val resourceFiles: @UnmodifiableView Map<ResourceLocation, Path>) {
    @Throws(IOException::class, PackExportException::class)
    fun export(emoteData: List<EmoteData>) {
        val filteredData = emoteData.stream()
            .filter { resourceFiles.containsKey(it.location) }.toList()
        if (filteredData.isEmpty()) {
            throw PackExportException("No emotes to export")
        }
        val configPath = configDir
        val exportPath = configPath!!.resolve("export")
        val exportFile: File = exportPath.toFile()
        if (!(exportFile.exists() || exportFile.mkdirs())) {
            throw IOException("Unable to create export directory")
        }
        PackWriter(exportPath.resolve("$name.zip").toFile(), PackType.RESOURCE).use { writer ->
            writer.write(
                PackMeta().description(
                    Component.literal("$name Emote Pack")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                )
            )

            // Write pack.png
            val iconOpt = modMetadata!!.getIconPath(128)
            if (iconOpt.isPresent) {
                var icon: String = iconOpt.get()
                if (!icon.startsWith("/")) icon = "/" + icon
                ChatEmotesMod::class.java.getResourceAsStream(icon).use { iconStream ->
                    if (iconStream == null) {
                        throw IOException("Unable to load icon")
                    }
                    writer.write("pack.png", iconStream)
                }
            }

            // Write translations
            val mfis: InputStream? =
                ChatEmotesMod::class.java.getResourceAsStream("/assets/$MOD_ID/lang/MANIFEST")
            if (mfis != null) {
                BufferedReader(InputStreamReader(mfis)).use { manifest ->
                    val lines: Iterator<String> = manifest.lines().iterator()
                    while (lines.hasNext()) {
                        val code: String = lines.next()
                        if (code.isEmpty()) continue
                        val lang: InputStream? =
                            ChatEmotesMod::class.java.getResourceAsStream("/assets/$MOD_ID/lang/$code.json")
                        if (lang != null) {
                            writer.write("assets/$MOD_ID/lang/$code.json", lang)
                        }
                    }
                }
            }

            val fontGen = FontGenerator(16, writer)
            for (data in filteredData) {
                val file = resourceFiles[data.location] ?: continue
                data.generateFonts(fontGen, configPath.resolve(file))
            }
        }
    }

    object ListSerializer : JsonSerializer<List<ResourcepackConfig>>, JsonDeserializer<List<ResourcepackConfig>> {
        override fun serialize(
            resourcepackConfigs: List<ResourcepackConfig>,
            type: Type,
            jsonSerializationContext: JsonSerializationContext
        ): JsonElement {
            val root = JsonObject()
            for (config: ResourcepackConfig in resourcepackConfigs) {
                val configRoot = JsonObject()
                for ((key, value) in config.resourceFiles.entries) {
                    configRoot.addProperty(key.toString(), value.toString())
                }
                root.add(config.name, configRoot)
            }
            return root
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            jsonElement: JsonElement,
            type: Type,
            jsonDeserializationContext: JsonDeserializationContext
        ): List<ResourcepackConfig> {
            val root = jsonElement.asJsonObject
            val configs = ArrayList<ResourcepackConfig>(root.size())
            for ((key, value) in root.entrySet()) {
                val filesRoot = value.asJsonObject
                val files = HashMap<ResourceLocation, Path>(filesRoot.size())
                for ((locStr, locPath) in filesRoot.entrySet()) {
                    val location: ResourceLocation
                    val path: Path
                    try {
                        location = ResourceLocation(locStr)
                    } catch (ex: ResourceLocationException) {
                        throw JsonSyntaxException(
                            "Invalid resource location '$locStr' in resourcepacks['$key']",
                            ex
                        )
                    }
                    if (!GsonHelper.isStringValue(locPath)) {
                        throw JsonSyntaxException(
                            "Expected resourcepacks['$key']['$locStr'] " +
                                    "to be a string, got " + GsonHelper.getType(locPath)
                        )
                    }
                    path = Path.of(locPath.asString)
                    files[location] = path
                }
                configs.add(ResourcepackConfig(key, Collections.unmodifiableMap(files)))
            }
            return configs
        }
    }

    companion object {
        @Suppress("UnstableApiUsage")
        val LIST_TYPE: Type = object : TypeToken<List<ResourcepackConfig?>?>() {}.type
    }
}