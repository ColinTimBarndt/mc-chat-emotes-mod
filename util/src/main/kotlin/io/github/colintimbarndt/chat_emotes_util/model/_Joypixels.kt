@file:JvmName("JoypixelsKt")

package io.github.colintimbarndt.chat_emotes_util.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias JoypixelsEmojiDataMap = Map<UnicodeSequence, JoypixelsEmojiData.Entry>

@Serializable(JoypixelsEmojiDataSerializer::class)
data class JoypixelsEmojiData(
    val map: JoypixelsEmojiDataMap
) : Map<UnicodeSequence, JoypixelsEmojiData.Entry> by map {
    @Serializable
    data class Entry(
        val name: String,
        val category: String,
        @SerialName("shortname")
        val shortName: String,
        @SerialName("shortname_alternates")
        val shortNameAlternates: Array<String>,
        @SerialName("unicode_output")
        val unicodeOutput: UnicodeSequence,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Entry

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }
}

fun JoypixelsEmojiData.toCompleteMap(): JoypixelsEmojiData {
    val clone = HashMap(map)
    map.values.forEach { clone[it.unicodeOutput] = it }
    return JoypixelsEmojiData(clone)
}

@Serializable
data class JoypixelsCategory(
    val order: Int,
    val category: String,
    @SerialName("category_label")
    val label: String,
)

private class JoypixelsEmojiDataSerializer : KSerializer<JoypixelsEmojiData> {
    private val innerSerializer = MapSerializer(UnicodeSequence.serializer(), JoypixelsEmojiData.Entry.serializer())
    override val descriptor: SerialDescriptor get() = innerSerializer.descriptor

    override fun deserialize(decoder: Decoder) = innerSerializer.deserialize(decoder).let(::JoypixelsEmojiData)

    override fun serialize(encoder: Encoder, value: JoypixelsEmojiData) = innerSerializer.serialize(encoder, value.map)
}
