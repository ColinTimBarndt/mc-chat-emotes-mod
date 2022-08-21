@file:JvmName("EmojiSourceMapperKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.model.UnicodeSequence
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface EmojiSourceMapper :
    NameMapper, CategoryMapper, AliasMapper, EmoticonMapper

fun interface NameMapper {
    fun nameFor(data: FlatEmojiData): String?
}

fun interface CategoryMapper {
    fun categoryFor(data: FlatEmojiData): String?
}

fun interface AliasMapper {
    fun aliasesFor(data: FlatEmojiData): List<String>
}

fun interface EmoticonMapper {
    fun emoticonsFor(data: FlatEmojiData): List<String>
}

internal val ALIAS_PATTERN = Regex("[a-z_-]+(:[a-z_-]+)*")

data class ComposedEmojiSourceMapper(
    val name: NameMapper,
    val category: CategoryMapper,
    val alias: AliasMapper,
    val emoticon: EmoticonMapper,
) : EmojiSourceMapper,
    NameMapper by name,
    CategoryMapper by category,
    AliasMapper by alias,
    EmoticonMapper by emoticon {
    companion object {
        suspend fun load(
            nameSource: EmojiNameSource,
            categorySource: EmojiCategorySource,
            aliasSource: EmojiAliasSource,
            emoticonSource: EmojiEmoticonSource
        ) = coroutineScope {
            val nameAsync = async { nameSource.load() }
            val categoryAsync = async { categorySource.load() }
            val aliasAsync = async { aliasSource.load() }
            val emoticonAsync = async { emoticonSource.load() }
            ComposedEmojiSourceMapper(
                nameAsync.await(),
                categoryAsync.await(),
                aliasAsync.await(),
                emoticonAsync.await()
            )
        }
    }
}

inline fun <T> Map<UnicodeSequence, T>.getWithFallback(emoji: FlatEmojiData): T? {
    var data = get(emoji.variation.unified)
    if (data == null && !emoji.isMainEmoji) {
        data = get(emoji.main.unified)
    }
    return data
}