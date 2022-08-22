@file:JvmName("ModelKt")

package io.github.colintimbarndt.chat_emotes.common.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.resources.ResourceLocation

private val colonRegex = Regex(":")

private val emptyArray = ArrayList<String>(0)

@Serializable
data class ChatEmote(
    val name: String? = null,
    val category: String? = null,
    val emoji: String? = null,
    val aliases: ArrayList<String> = emptyArray,
    val emoticons: ArrayList<String> = emptyArray,
    val char: Char,
    @Serializable(ResourceLocationDeserializer::class)
    val font: ResourceLocation,
) {
    @Transient
    val aliasWithColons =
        if (aliases.isEmpty()) null
        else {
            val str = aliases[0].replace(colonRegex, "::")
            ":$str:"
        }

    @Transient
    val aliasesWithInnerColons = aliases.map {
        it.replace(colonRegex, "::")
    }
}

private class ResourceLocationDeserializer : KSerializer<ResourceLocation> {
    private val serializer = String.serializer()
    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun deserialize(decoder: Decoder) = ResourceLocation(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: ResourceLocation) {
        encoder.encodeString(value.toString())
    }

}