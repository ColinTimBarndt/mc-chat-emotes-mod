@file:JvmName("PackGenerationKt")
@file:Suppress("NOTHING_TO_INLINE")
@file:JvmMultifileClass

package io.github.colintimbarndt.chat_emotes_util.serial

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToStream
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PackWriter private constructor(
    private val zipFile: ZipOutputStream
) : Closeable, AutoCloseable {
    init {
        zipFile.setMethod(ZipOutputStream.DEFLATED)
    }

    override fun close() = zipFile.close()

    inner class PackedFile(name: String) : OutputStream() {
        init {
            zipFile.putNextEntry(ZipEntry(name))
        }

        private var closed = false

        private inline fun ensureOpen() {
            if (closed) throw IOException("Stream closed")
        }

        override fun write(data: Int) {
            ensureOpen()
            zipFile.write(data)
        }

        override fun write(b: ByteArray) {
            ensureOpen()
            zipFile.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            ensureOpen()
            zipFile.write(b, off, len)
        }

        override fun flush() = ensureOpen()

        override fun close() {
            if (closed) return
            zipFile.closeEntry()
        }
    }

    companion object {
        fun of(stream: OutputStream) = PackWriter(ZipOutputStream(stream))
        inline fun of(file: File) = of(file.outputStream())
    }
}

inline fun PackWriter.addFile(name: String, write: (PackWriter.PackedFile) -> Unit) {
    PackedFile(name).use(write)
}

inline fun PackWriter.addFile(name: String, stream: InputStream) {
    stream.transferTo(PackedFile(name))
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> PackWriter.addJsonFile(name: String, value: T, json: Json = Json) {
    json.encodeToStream(value, PackedFile(name))
}

inline fun PackWriter.addMetadata(apply: RootPackMetadata.() -> Unit) = addMetadata(Json, apply)

inline fun PackWriter.addMetadata(json: Json, apply: RootPackMetadata.() -> Unit) {
    val meta = RootPackMetadata()
    meta.apply()
    addJsonFile("pack.mcmeta", meta, json)
}

private val emptyDescription = JsonPrimitive("")

@Serializable
class RootPackMetadata {
    val pack: PackMetadata = PackMetadata()
}

@Serializable
class PackMetadata {
    @SerialName("pack_format")
    var format: Int = -1
    var description: JsonElement = emptyDescription
}
