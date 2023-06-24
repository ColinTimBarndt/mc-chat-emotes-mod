@file:JvmName("ModelKt")

package io.github.colintimbarndt.chat_emotes.common.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private val colonRegex = Regex(":")

private val emptyArrayList = ArrayList<String>(0)

@Serializable(ResourceLocationDeserializer::class)
data class ResourceLocation(
    val namespace: String,
    val path: String,
) {
    init {
        assert(isValidNamespace(namespace))
        assert(isValidPath(path))
    }

    constructor(full: String) : this(getNamespace(full), getPath(full))

    companion object {
        fun isValidPath(string: String): Boolean {
            for (element in string) {
                if (!validPathChar(element)) {
                    return false
                }
            }
            return true
        }

        fun isValidNamespace(string: String): Boolean {
            for (element in string) {
                if (!validNamespaceChar(element)) {
                    return false
                }
            }
            return true
        }

        private fun validPathChar(c: Char): Boolean {
            return (c == '_' || c == '-' || c in 'a'..'z' || c in '0'..'9') || c == '/' || c == '.'
        }

        private fun validNamespaceChar(c: Char): Boolean {
            return (c == '_' || c == '-' || c in 'a'..'z' || c in '0'..'9') || c == '.'
        }

        private fun getNamespace(full: String): String {
            val idx = full.indexOf(':')
            return if (idx == -1) "minecraft" else full.substring(0, idx)
        }

        private fun getPath(full: String): String {
            val idx = full.indexOf(':')
            return full.substring(idx + 1)
        }
    }
}

@Serializable
data class ChatEmote(
    val name: String? = null,
    val category: String? = null,
    val emoji: String? = null,
    val aliases: ArrayList<String> = emptyArrayList,
    val emoticons: ArrayList<String> = emptyArrayList,
    val char: Char,
    val font: ResourceLocation,
) {
    @Transient
    val aliasesWithInnerColons = aliases.map {
        it.replace(colonRegex, "::")
    }

    @Transient
    val aliasWithColons = aliasesWithInnerColons.firstOrNull()?.let { ":$it:" }
}

private class ResourceLocationDeserializer : KSerializer<ResourceLocation> {
    private val serializer = String.serializer()
    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun deserialize(decoder: Decoder): ResourceLocation {
        val str = decoder.decodeString()
        val idx = str.indexOf(':')
        return if (idx == -1) ResourceLocation("minecraft", str)
        else ResourceLocation(str.substring(0, idx), str.substring(idx + 1))
    }

    override fun serialize(encoder: Encoder, value: ResourceLocation) {
        encoder.encodeString(value.toString())
    }
}