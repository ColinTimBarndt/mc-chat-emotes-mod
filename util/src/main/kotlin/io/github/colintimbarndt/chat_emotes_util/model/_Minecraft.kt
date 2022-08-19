@file:JvmName("MinecraftKt")

package io.github.colintimbarndt.chat_emotes_util.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private val emptyDescription = JsonPrimitive("")

@Serializable
class PackMetadata {
    val pack: Pack = Pack()

    @Serializable
    class Pack {
        @SerialName("pack_format")
        var format: Int = -1
        var description: JsonElement = emptyDescription
    }
}

@Serializable
internal class FontMetadata(
    val providers: ArrayList<Provider>
) {
    constructor() : this(arrayListOf())

    @Serializable
    internal sealed class Provider

    @Serializable
    @SerialName("minecraft:bitmap")
    internal data class BitmapProvider(
        val file: String,
        val chars: Array<CharArrayString>,
        val ascent: Int,
        val height: Int,
    ) : Provider() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BitmapProvider

            if (file != other.file) return false
            if (ascent != other.ascent) return false
            if (height != other.height) return false
            if (!chars.contentDeepEquals(other.chars)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = file.hashCode()
            result = 31 * result + ascent
            result = 31 * result + height
            result = 31 * result + chars.contentDeepHashCode()
            return result
        }
    }
}