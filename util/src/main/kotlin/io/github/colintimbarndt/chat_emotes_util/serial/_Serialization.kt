@file:JvmName("SerializationKt")

package io.github.colintimbarndt.chat_emotes_util.serial

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.internal.MapLikeSerializer
import javax.naming.OperationNotSupportedException

@JvmInline
@Serializable(SequenceSerializer::class)
internal value class SerializableSequence<T>(val delegate: Sequence<T>) : Sequence<T> by delegate

internal class SequenceSerializer<T>(val elementSerializer: KSerializer<T>) : KSerializer<SerializableSequence<T>> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("Sequence", ListSerializer(elementSerializer).descriptor)

    override fun deserialize(decoder: Decoder): SerializableSequence<T> {
        throw UnsupportedOperationException()
    }

    override fun serialize(encoder: Encoder, value: SerializableSequence<T>) {
        encoder.encodeCollection(descriptor, Int.MAX_VALUE) {
            value.forEachIndexed { i, element ->
                encodeSerializableElement(descriptor, i, elementSerializer, element)
            }
        }
    }

}

internal class UnicodeSequenceSerializer : KSerializer<UnicodeSequence> {
    override val descriptor = PrimitiveSerialDescriptor("UnicodeSequence", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UnicodeSequence {
        val sb = StringBuilder(16)
        val src = decoder.decodeString().splitToSequence('-')
        src
            .forEach { sb.appendCodePoint(it.toInt(16)) }
        return UnicodeSequence(sb.toString())
    }

    override fun serialize(encoder: Encoder, value: UnicodeSequence) {
        val sb = StringBuilder(value.length * 6)
        sb.codePoints().forEach { sb.append(it.toString(16)) }
    }
}