package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.model.EmojiData
import io.github.colintimbarndt.chat_emotes_util.web.GithubFile
import io.github.colintimbarndt.chat_emotes_util.web.WebHelper.STANDARD_CACHE_TIME
import io.github.colintimbarndt.chat_emotes_util.web.getInputStreamSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence

object EmojiDataProvider {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val source = GithubFile(
        "iamcal",
        "emoji-data",
        "master",
        "emoji.json"
    )

    private var cache: ArrayList<EmojiData>? = null

    suspend inline fun loadSequence() = load().asSequence()

    suspend inline fun load(): ArrayList<EmojiData> = withContext(Dispatchers.IO) {
        loadSync()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Synchronized
    fun loadSync(): ArrayList<EmojiData> {
        cache?.let { return it }
        val data = ArrayList<EmojiData>(4096)
        json.decodeToSequence<EmojiData>(
            source.getInputStreamSync(STANDARD_CACHE_TIME).result,
            DecodeSequenceMode.ARRAY_WRAPPED
        ).forEach { data += it }
        cache = data
        return data
    }
}