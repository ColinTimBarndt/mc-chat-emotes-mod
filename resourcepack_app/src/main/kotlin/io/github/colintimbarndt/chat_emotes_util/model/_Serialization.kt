@file:JvmName("SerializationKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.model

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

/**
 * This is a wrapper which enables Kotlin to encode a sequence as an array.
 * This works with Json, but there will most likely be problems with other formats.
 */
@JvmInline
@Serializable
value class SerializableSequence<T>(
    @Serializable(SequenceSerializer::class)
    private val delegate: Sequence<T>
) : Sequence<T> by delegate

private class SequenceSerializer<T>(private val elementSerializer: KSerializer<T>) : KSerializer<Sequence<T>> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("Sequence", ListSerializer(elementSerializer).descriptor)

    override fun deserialize(decoder: Decoder): Sequence<T> {
        throw UnsupportedOperationException()
    }

    override fun serialize(encoder: Encoder, value: Sequence<T>) {
        // MAX_VALUE is used to provoke an exception when this value is used by a serializer
        // Better assume too much as a size than not enough,
        // and the size of a Sequence is unknown before iterating it.
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
    override fun deserialize(decoder: Decoder) = URI(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: URI) = encoder.encodeString(value.toString())
}

class ResourceKeySerializer : KSerializer<ResourceKey> {
    override val descriptor = String.serializer().descriptor
    override fun deserialize(decoder: Decoder) = ResourceKey.of(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: ResourceKey) = encoder.encodeString(value.toString())
}