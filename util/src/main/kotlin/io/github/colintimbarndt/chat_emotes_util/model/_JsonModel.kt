@file:JvmName("JsonModelKt")

package io.github.colintimbarndt.chat_emotes_util.model

import io.github.colintimbarndt.chat_emotes_util.web.FileSource
import kotlinx.serialization.Serializable

@Serializable
data class ChatEmoteData(
    val name: String?,
    val category: String?,
    val emoji: String,
    val aliases: List<String> = emptyList(),
    val emoticons: List<String> = emptyList(),
    val char: Char,
    val font: ResourceKey,
)

@Serializable
data class Attribution(
    val name: String,
    val source: FileSource,
    val license: FileSource? = null,
)

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