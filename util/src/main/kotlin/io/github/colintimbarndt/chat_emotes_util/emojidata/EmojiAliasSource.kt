package io.github.colintimbarndt.chat_emotes_util.emojidata

enum class EmojiAliasSource(private val label: String) {
    None("None") {
        private val empty = emptyArray<String>()

        override suspend fun load() = AliasMapper { empty }
    },
    EmojiData("Emoji Data") {
        override suspend fun load(): AliasMapper {
            lateinit var mainAliases: Array<String>
            return AliasMapper { data ->
                if (data.variationCombo == null) {
                    mainAliases = data.main.shortNames.toTypedArray()
                    for ((i, alias) in mainAliases.withIndex()) {
                        mainAliases[i] = alias.replace('-', '_')
                    }
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
                    Array(mainAliases.size) { mainAliases[it] + suffix }
                }
            }
        }
    },
    Joypixels("Joypixels / Discord") {
        override suspend fun load(): AliasMapper {
            TODO("Not yet implemented")
        }
    };

    override fun toString() = label

    abstract suspend fun load(): AliasMapper

    fun interface AliasMapper {
        fun aliasesFor(data: FlatEmojiData): Array<String>
    }

    companion object {
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
    }
}