package io.github.colintimbarndt.chat_emotes.common.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.github.colintimbarndt.chat_emotes.common.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.common.util.BomAwareReader.createBuffered
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

abstract class EmoteDataLoaderBase {
    var loadedEmoteData: List<EmoteData> = emptyList()
        protected set
    protected val resourceLoaderIdentifier by lazy { ResourceLocation(MOD_ID, "emotes") }
    private val gson = GsonBuilder()
        .registerTypeAdapter(ResourceLocation::class.java, ResourceLocation.Serializer())
        .create()
    protected abstract val serverMod: ChatEmotesServerModBase

    fun load(
        manager: ResourceManager,
        profiler: ProfilerFiller,
        executor: Executor
    ): CompletableFuture<List<EmoteData>> {
        val resources = manager.listResources("emote") { it.path.endsWith(".json") }
        val samples = manager.listResources("emote") { it.path.endsWith(".txt") }
        val dataList = ArrayList<EmoteData>(resources.size)
        for ((id, value) in resources) {
            try {
                val json = gson.fromJson(value.openAsReader(), JsonObject::class.java)
                val type = gson.fromJson(json["type"], ResourceLocation::class.java)
                val serializer = serverMod.registries.emoteDataSerializers[type]
                if (serializer == null) {
                    LOGGER.warn("Unknown emote data type {}", type)
                    continue
                }
                val samplesResource: Resource?
                val samplesResourceLocation: ResourceLocation
                run {
                    val path = id.path
                    val loc = ResourceLocation(
                        id.namespace,
                        path.substring(0, path.length - 5) + ".txt"
                    )
                    samplesResource = samples[loc]
                    samplesResourceLocation = loc
                }
                var sampleStrings: Set<String>? = null
                if (samplesResource != null) {
                    try {
                        sampleStrings = readSamples(samplesResource)
                    } catch (ex: IOException) {
                        LOGGER.error("Unable to load resource {}", samplesResourceLocation, ex)
                    }
                }
                val emoteId: ResourceLocation
                run {
                    val path = id.path
                    emoteId = ResourceLocation(
                        id.namespace,
                        path.substring(6, path.length - 5)
                    )
                }
                val eData = serializer.read(emoteId, json, sampleStrings)
                dataList.add(eData)
            } catch (ex: IOException) {
                LOGGER.error("Unable to load resource {}", id, ex)
            } catch (ex: JsonIOException) {
                LOGGER.error("Unable to load resource {}", id, ex)
            } catch (ex: JsonSyntaxException) {
                LOGGER.error("Unable to load resource {}", id, ex)
            }
        }
        return CompletableFuture.completedFuture(dataList)
    }

    fun apply(
        data: List<EmoteData>,
        manager: ResourceManager,
        profiler: ProfilerFiller,
        executor: Executor
    ): CompletableFuture<Void> {
        loadedEmoteData = Collections.unmodifiableList(data)
        LOGGER.info("Loaded {} emotes", data.stream().mapToInt { d: EmoteData -> d.emotes.size }.sum())
        return CompletableFuture.completedFuture(null)
    }

    @Throws(IOException::class)
    private fun readSamples(resource: Resource): Set<String> {
        val results = HashSet<String>()
        createBuffered(resource.open(), 64).use { stream ->
            val lines = stream.lines().iterator()
            while (lines.hasNext()) {
                val line = lines.next()
                if (line.isNotEmpty()) {
                    results.add(line)
                }
            }
        }
        return Collections.unmodifiableSet(results)
    }
}