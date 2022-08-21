package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.DefaultedEnum
import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.model.DiscordEmojiMap

enum class EmojiNameSource(override val label: String) : Labeled {
    Raw("Emoji Data (Raw)") {
        override suspend fun load() = rawMapper
    },
    EmojiData("Emoji Data") {
        override suspend fun load() = NameMapper { it.name.lowercase() }
    },
    Joypixels("Joypixels") {
        override suspend fun load(): NameMapper {
            val data = JoypixelsDataProvider.loadAliases()
            return NameMapper {
                data.getWithFallback(it)?.name
            }
        }
    },
    Discord("Discord") {
        override suspend fun load(): NameMapper = DiscordDataProvider.Version.Stable.load().let(::DiscordNameMapper)
    },
    DiscordCanary("Discord (Canary)") {
        override suspend fun load(): NameMapper = DiscordDataProvider.Version.Canary.load().let(::DiscordNameMapper)
    },
    ;

    abstract suspend fun load(): NameMapper

    private class DiscordNameMapper(private val dcData: DiscordEmojiMap) : NameMapper {
        override fun nameFor(data: FlatEmojiData) = dcData[data.variation.unified]?.primaryName
    }

    companion object : DefaultedEnum<EmojiNameSource> {
        val rawMapper = NameMapper { it.name }
        override val default = EmojiData
        override fun values() = EmojiNameSource.values()
    }
}