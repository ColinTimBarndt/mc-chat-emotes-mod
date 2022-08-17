package io.github.colintimbarndt.chat_emotes.common.data

import net.minecraft.resources.ResourceLocation

data class Emote(
    val font: ResourceLocation,
    val character: Char,
    val aliases: Array<String>,
    /**
     * ASCII [Emoticons](https://www.unicode.org/reports/tr51/proposed.html#Emoticons)
     */
    val emoticons: Array<String>,
    val unicodeSequence: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Emote

        if (font != other.font) return false
        if (character != other.character) return false
        if (!aliases.contentEquals(other.aliases)) return false
        if (!emoticons.contentEquals(other.emoticons)) return false
        if (unicodeSequence != other.unicodeSequence) return false

        return true
    }

    override fun hashCode(): Int {
        var result = font.hashCode()
        result = 31 * result + character.hashCode()
        result = 31 * result + aliases.contentHashCode()
        result = 31 * result + emoticons.contentHashCode()
        result = 31 * result + (unicodeSequence?.hashCode() ?: 0)
        return result
    }
}