package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.DefaultedEnum
import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.model.DiscordEmojiMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

enum class EmojiCategorySource(override val label: String) : Labeled {
    EmojiData("Emoji Data") {
        override suspend fun load() = rawMapper
    },
    Joypixels("Joypixels") {
        override suspend fun load(): CategoryMapper {
            val aliases = JoypixelsDataProvider.loadAliases()
            return CategoryMapper {
                aliases[it.main.unified]?.category
            }
        }
    },
    JoypixelsLabeled("Joypixels (labels)") {
        override suspend fun load(): CategoryMapper = coroutineScope {
            val aliasesAsync = async { JoypixelsDataProvider.loadAliases() }
            val categoriesAsync = async { JoypixelsDataProvider.loadCategories() }
            val aliases = aliasesAsync.await()
            val categories = categoriesAsync.await()
            CategoryMapper {
                aliases[it.main.unified]?.category?.let(categories::get)?.label
            }
        }
    },
    DiscordStable("Discord") {
        override suspend fun load(): CategoryMapper = DiscordDataProvider.Version.Stable.load().let(::DiscordCategoryMapper)
    },
    DiscordCanary("Discord (Canary)") {
        override suspend fun load(): CategoryMapper = DiscordDataProvider.Version.Canary.load().let(::DiscordCategoryMapper)
    },
    ;

    abstract suspend fun load(): CategoryMapper

    private class DiscordCategoryMapper(private val dcData: DiscordEmojiMap) : CategoryMapper {
        override fun categoryFor(data: FlatEmojiData) = dcData.getWithFallback(data)?.category
    }

    companion object : DefaultedEnum<EmojiCategorySource> {
        val rawMapper = CategoryMapper { it.category }
        override val default = EmojiData
        override fun values() = EmojiCategorySource.values()
    }
}