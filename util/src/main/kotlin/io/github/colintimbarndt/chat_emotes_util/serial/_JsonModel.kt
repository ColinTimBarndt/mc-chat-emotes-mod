@file:JvmName("JsonModelKt")

package io.github.colintimbarndt.chat_emotes_util.serial

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatEmoteData(
    val name: String,
    val emoji: String,
    val aliases: List<String>,
    val char: Char,
)

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
@Serializable(UnicodeSequenceSerializer::class)
value class UnicodeSequence(private val delegate: String) : CharSequence by delegate {
    override fun toString() = delegate
}