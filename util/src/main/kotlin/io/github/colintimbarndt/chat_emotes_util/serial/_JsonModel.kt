@file:JvmName("JsonModelKt")

package io.github.colintimbarndt.chat_emotes_util.serial

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatEmoteData(
    val name: String,
    val emoji: String,
    val aliases: Array<String>,
    val char: Char,
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

@Serializable
sealed class BaseEmojiData {
    abstract val unified: UnicodeSequence
    abstract val nonQualified: UnicodeSequence?
    abstract val image: String
    abstract val sheetX: UInt
    abstract val sheetY: UInt
}

@Serializable
@JvmInline
value class JoypixelsEmojiData(
    val map: Map<UnicodeSequence, JoypixelsEmojiDataEntry>
)

@Serializable
data class JoypixelsEmojiDataEntry(
    val name: String,
    val category: String,
    @SerialName("shortname")
    val shortName: String,
    @SerialName("shortname_alternatives")
    val shortNameAlternatives: List<String>,
)

@Serializable
data class SimpleEmojiData(
    override val unified: UnicodeSequence,
    @SerialName("non_qualified")
    override val nonQualified: UnicodeSequence?,
    override val image: String,
    @SerialName("sheet_x")
    override val sheetX: UInt,
    @SerialName("sheet_y")
    override val sheetY: UInt,
) : BaseEmojiData()

@Serializable
data class EmojiData(
    override val unified: UnicodeSequence,
    @SerialName("non_qualified")
    override val nonQualified: UnicodeSequence?,
    override val image: String,
    @SerialName("sheet_x")
    override val sheetX: UInt,
    @SerialName("sheet_y")
    override val sheetY: UInt,

    val name: String,
    @SerialName("short_name")
    val shortName: String,
    @SerialName("short_names")
    val shortNames: ArrayList<String>,
    val category: String,
    val subcategory: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int,
    @SerialName("skin_variations")
    val skinVariations: HashMap<UnicodeSequence, SimpleEmojiData>? = null,
) : BaseEmojiData()

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