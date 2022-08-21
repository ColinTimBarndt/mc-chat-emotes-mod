@file:OptIn(ExperimentalSerializationApi::class)

package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.model.JoypixelsCategory
import io.github.colintimbarndt.chat_emotes_util.model.JoypixelsEmojiData
import io.github.colintimbarndt.chat_emotes_util.model.toCompleteMap
import io.github.colintimbarndt.chat_emotes_util.web.GithubFile
import io.github.colintimbarndt.chat_emotes_util.web.WebHelper.STANDARD_CACHE_TIME
import io.github.colintimbarndt.chat_emotes_util.web.getInputStreamSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.decodeToSequence

object JoypixelsDataProvider {
    private val json = Json { ignoreUnknownKeys = true }

    private val aliasesSource = GithubFile(
        "joypixels",
        "emoji-toolkit",
        "master",
        "emoji_strategy.json"
    )
    private val categoriesSource = GithubFile(
        "joypixels",
        "emoji-toolkit",
        "master",
        "categories.json"
    )

    private var aliasesCache: JoypixelsEmojiData? = null
    private var categoriesCache: Map<String, JoypixelsCategory>? = null

    suspend inline fun loadAliases() = withContext(Dispatchers.IO) {
        loadAliasesSync()
    }

    suspend inline fun loadCategories() = withContext(Dispatchers.IO) {
        loadCategoriesSync()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Synchronized
    fun loadAliasesSync(): JoypixelsEmojiData {
        aliasesCache?.let { return it }
        val loaded =
            json.decodeFromStream<JoypixelsEmojiData>(aliasesSource.getInputStreamSync(STANDARD_CACHE_TIME).result)
                .toCompleteMap()
        aliasesCache = loaded
        return loaded
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Synchronized
    fun loadCategoriesSync(): Map<String, JoypixelsCategory> {
        categoriesCache?.let { return it }
        val loaded =
            json.decodeToSequence<JoypixelsCategory>(
                categoriesSource.getInputStreamSync(STANDARD_CACHE_TIME).result,
                DecodeSequenceMode.ARRAY_WRAPPED
            ).associateBy(JoypixelsCategory::category)
        categoriesCache = loaded
        return loaded
    }
}