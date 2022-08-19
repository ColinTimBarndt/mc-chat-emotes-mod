@file:JvmName("JoyPixelsKt")

package io.github.colintimbarndt.chat_emotes_util.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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