@file:JvmName("SerializationKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.serial

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import java.net.URI

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

internal class UnicodeSequenceSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("UnicodeSequence", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val str = decoder.decodeString()
        return UnicodeSequence.parse(str).toString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        val sb = StringBuilder(value.length * 6)
        sb.codePoints().forEach { sb.append(it.toString(16)) }
    }
}

@JvmInline
@Serializable
value class CharArrayString(
    @Serializable(CharArrayStringSerializer::class)
    val delegate: CharArray
) : CharSequence {
    constructor(size: Int) : this(CharArray(size))
    constructor(size: Int, init: (Int) -> Char) : this(CharArray(size, init))
    val size inline get() = delegate.size
    override val length: Int get() = size
    override operator fun get(index: Int) = delegate[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharArrayString =
        CharArrayString(delegate.sliceArray(startIndex until endIndex))

    inline operator fun set(idx: Int, value: Char) = delegate.set(idx, value)

    override fun toString() = delegate.concatToString()
}

class CharArrayStringSerializer : KSerializer<CharArray> {
    override val descriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder): CharArray = decoder.decodeString().toCharArray()
    override fun serialize(encoder: Encoder, value: CharArray) = encoder.encodeString(CharArrayString(value).toString())
}

class UriSerializer : KSerializer<URI> {
    override val descriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder): URI = URI(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: URI) = encoder.encodeString(value.toString())
}