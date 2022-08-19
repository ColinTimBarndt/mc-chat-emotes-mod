@file:JvmName("JsonModelKt")

package io.github.colintimbarndt.chat_emotes_util.model

import kotlinx.serialization.Serializable

// TODO: Move into common library
@Serializable
data class ChatEmoteData(
    val name: String,
    val emoji: String,
    val aliases: Array<String>,
    val char: Char,
    val font: ResourceKey,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatEmoteData

        if (name != other.name) return false
        if (emoji != other.emoji) return false
        if (!aliases.contentEquals(other.aliases)) return false
        if (char != other.char) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + emoji.hashCode()
        result = 31 * result + aliases.contentHashCode()
        result = 31 * result + char.hashCode()
        return result
    }
}

/**
 * A unicode sequence is encoded as its hexadecimal unicode code points joined by hyphens (or any other delimiter)
 */
@JvmInline
@Serializable
value class UnicodeSequence(
    @Serializable(UnicodeSequenceSerializer::class)
    private val delegate: String
) : CharSequence by delegate {
    override fun toString() = delegate

    companion object {
        private val defaultDelimiters = charArrayOf('-', '_', ' ').apply { sort() }
        fun parse(str: String, vararg delimiters: Char = defaultDelimiters): UnicodeSequence {
            val sb = StringBuilder(16)
            val src = str.splitToSequence(*delimiters)
            src.forEach { sb.appendCodePoint(it.toInt(16)) }
            return UnicodeSequence(sb.toString())
        }
    }
}