@file:Suppress("UNUSED_PARAMETER")

package io.github.colintimbarndt.chat_emotes.common.data

import io.github.colintimbarndt.chat_emotes.common.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import io.github.colintimbarndt.chat_emotes.common.NAMESPACE
import io.github.colintimbarndt.chat_emotes.common.util.Futures
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

abstract class EmoteDataLoaderBase {
    var loadedEmoteData: List<EmoteDataBundle> = emptyList()
        protected set
    var maxCombinedEmote: Int = 0
        protected set
    protected val resourceLoaderIdentifier = ResourceLocation(NAMESPACE, "emotes")
    protected abstract val serverMod: ChatEmotesServerModBase

    fun load(
        manager: ResourceManager,
        profiler: ProfilerFiller,
        executor: Executor
    ): CompletableFuture<List<EmoteDataBundle>> = Futures.supplyAsync(executor) {
        val resources = manager.listResources("emote") { it.path.endsWith(".json", true) }
        val dataList = ArrayList<EmoteDataBundle>(resources.size)

        for ((id, resource) in resources) {
            try {
                val resourceLocation: ResourceLocation
                val path = id.path
                resourceLocation = ResourceLocation(
                    id.namespace,
                    path.substring(6, path.length - ".json".length)
                )
                dataList += loadBundle(resource, resourceLocation)
            } catch (ex: Throwable) {
                LOGGER.error("Unable to load resource {}", id, ex)
            }
        }
        dataList
    }

    fun apply(
        data: List<EmoteDataBundle>,
        manager: ResourceManager,
        profiler: ProfilerFiller,
        executor: Executor
    ): CompletableFuture<Void> {
        maxCombinedEmote = data.maxOfOrNull(EmoteDataBundle::maxCombinedEmote) ?: 1
        loadedEmoteData = Collections.unmodifiableList(data)
        LOGGER.info("Loaded {} emotes", data.asSequence().map { it.size }.sum())
        return CompletableFuture.completedFuture(null)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun loadBundle(resource: Resource, resourceLocation: ResourceLocation): EmoteDataBundle {
        val emotes = resource.open().use {
            Json.decodeFromStream<ArrayList<ChatEmote>>(it)
        }
        return EmoteDataBundle(resourceLocation, emotes)
    }
}