package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.model.DiscordEmojiList
import io.github.colintimbarndt.chat_emotes_util.model.DiscordEmojiMap
import io.github.colintimbarndt.chat_emotes_util.model.toMap
import io.github.colintimbarndt.chat_emotes_util.web.WebHelper
import io.github.colintimbarndt.chat_emotes_util.web.WebHelper.STANDARD_CACHE_TIME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object DiscordDataProvider {
    private val json = Json { ignoreUnknownKeys = true }

    private val cache = ConcurrentHashMap<Version, DiscordEmojiMap>(Version.values().size)

    suspend inline fun load(version: Version) = withContext(Dispatchers.IO) {
        loadSync(version)
    }

    @Synchronized
    fun loadSync(version: Version): DiscordEmojiMap {
        cache[version]?.let { return it }
        val data = loadSync(version.source)
        cache[version] = data
        return data
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadSync(source: URI) =
        WebHelper.getInputStreamSync(source, STANDARD_CACHE_TIME).result.use {
            json.decodeFromStream<DiscordEmojiList>(it)
        }.toMap()

    enum class Version(internal val source: URI) {
        Stable(URI("https://emzi0767.gl-pages.emzi0767.dev/discord-emoji/discordEmojiMap.min.json")),
        Canary(URI("https://emzi0767.gl-pages.emzi0767.dev/discord-emoji/discordEmojiMap-canary.min.json")),
        ;

        suspend inline fun load() = load(this)
    }
}