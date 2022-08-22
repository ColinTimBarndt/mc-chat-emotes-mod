package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.DefaultedEnum
import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.model.DiscordEmojiMap

enum class EmojiEmoticonSource(override val label: String) : Labeled {
    None("None") {
        override suspend fun load() = noneMapper
    },
    EmojiData("Emoji Data") {
        override suspend fun load() = EmoticonMapper {
            if (it.isMainEmoji) {
                it.main.texts
            } else {
                emptyList()
            }
        }
    },

    // TODO: Joypixels
    DiscordStable("Discord") {
        override suspend fun load(): EmoticonMapper = DiscordDataProvider.Version.Stable.load().let(::DiscordEmoticonMapper)
    },
    DiscordCanary("Discord (Canary)") {
        override suspend fun load(): EmoticonMapper = DiscordDataProvider.Version.Canary.load().let(::DiscordEmoticonMapper)
    },
    ;

    abstract suspend fun load(): EmoticonMapper

    private class DiscordEmoticonMapper(private val dcData: DiscordEmojiMap) : EmoticonMapper {
        override fun emoticonsFor(data: FlatEmojiData) =
            dcData[data.variation.unified]?.names?.filterNot(ALIAS_PATTERN::matches) ?: emptyList()
    }

    companion object : DefaultedEnum<EmojiEmoticonSource> {
        val noneMapper = EmoticonMapper { emptyList() }
        override val default = None
        override fun values() = EmojiEmoticonSource.values()
    }
}