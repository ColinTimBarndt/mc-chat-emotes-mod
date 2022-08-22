package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.DefaultedEnum
import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.model.DiscordEmojiMap

enum class EmojiAliasSource(override val label: String) : Labeled {
    None("None") {
        override suspend fun load() = noneMapper
    },
    EmojiData("Emoji Data") {
        override suspend fun load(): AliasMapper {
            lateinit var mainAliases: ArrayList<String>
            return AliasMapper { data ->
                if (data.variationCombo == null) {
                    mainAliases = data.main.shortNames
                    mainAliases
                } else {
                    // Skin tone = 2 chars, `_toneX` = 6 chars
                    val suffix0 = StringBuilder(3 * data.variationCombo.length)
                    data.variationCombo.codePoints().forEach {
                        // See https://www.unicode.org/reports/tr51/#Diversity
                        if (it !in SKIN_TONE_LIGHT..SKIN_TONE_DARK) {
                            throw RuntimeException("invalid skin tone ${it.toChar()} (0x${it.toString(16)})")
                        }
                        val tone = (it + SKIN_TONE_TO_DIGIT).toChar()
                        suffix0.append("_tone").append(tone)
                    }
                    val suffix = suffix0.toString()

                    val list = ArrayList<String>(mainAliases.size)
                    for (i in 0 until mainAliases.size) list[i] = mainAliases[i] + suffix
                    list
                }
            }
        }
    },
    Joypixels("Joypixels") {
        override suspend fun load(): AliasMapper {
            val data = JoypixelsDataProvider.loadAliases()
            return AliasMapper {
                val entry = data[it.variation.unified]
                    ?: return@AliasMapper emptyList()
                arrayListOf(entry.shortName, *entry.shortNameAlternates).stripColons()
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun ArrayList<String>.stripColons(): ArrayList<String> {
            forEachIndexed { index, s ->
                if (s.length >= 2 && s[0] == ':' && s[s.length - 1] == ':') {
                    set(index, s.substring(1 until s.length - 1))
                }
            }
            return this
        }
    },
    Discord("Discord") {
        override suspend fun load(): AliasMapper = DiscordDataProvider.Version.Stable.load().let(::DiscordAliasMapper)
    },
    DiscordCanary("Discord (Canary)") {
        override suspend fun load(): AliasMapper = DiscordDataProvider.Version.Canary.load().let(::DiscordAliasMapper)
    },
    ;

    abstract suspend fun load(): AliasMapper

    private class DiscordAliasMapper(private val dcData: DiscordEmojiMap) : AliasMapper {
        override fun aliasesFor(data: FlatEmojiData) =
            dcData[data.variation.unified]?.names?.filter(ALIAS_PATTERN::matches) ?: emptyList()
    }

    companion object : DefaultedEnum<EmojiAliasSource> {
        /**
         * Unicode code point `U+1F3FB` (🏻)
         */
        private const val SKIN_TONE_LIGHT = 0x1F3FB

        /**
         * Unicode code point `U+1F3FF` (🏿)
         */
        private const val SKIN_TONE_DARK = 0x1F3FF

        /**
         * When added to a skin tone code point, it converts it to a digit character code (1..5)
         */
        private const val SKIN_TONE_TO_DIGIT = '1'.code - SKIN_TONE_LIGHT

        val noneMapper = AliasMapper { emptyList() }
        override val default = EmojiData
        override fun values() = EmojiAliasSource.values()
    }
}