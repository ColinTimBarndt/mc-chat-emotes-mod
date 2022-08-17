package io.github.colintimbarndt.chat_emotes_util.emojidata

enum class EmojiAliasSource(private val label: String) {
    EmojiData("Emoji Data"),
    Joypixels("Joypixels / Discord");

    override fun toString() = label
}