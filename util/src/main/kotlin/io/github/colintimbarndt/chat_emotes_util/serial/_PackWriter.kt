@file:JvmName("PackWriterKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.serial

import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.model.PackMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface PackWriter : Closeable, AutoCloseable {
    fun addFile(name: String): OutputStream

    companion object {
        val values = arrayListOf(
            ZipPackWriter,
            FolderPackWriter
        )
    }
}

interface PackWriterFactory : Labeled {
    val fileType: FileType
    fun create(file: File): PackWriter
}

inline fun PackWriter.addFile(name: String, write: (OutputStream) -> Unit) {
    addFile(name).use(write)
}

inline fun PackWriter.addFile(name: String, stream: InputStream) = addFile(name, stream::transferTo)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> PackWriter.addJsonFile(name: String, value: T, json: Json = Json) {
    json.encodeToStream(value, addFile(name))
}

inline fun PackWriter.addMetadata(meta: PackMetadata, json: Json = Json) = addJsonFile("pack.mcmeta", meta, json)

inline fun PackWriter.addMetadata(apply: PackMetadata.() -> Unit) = addMetadata(Json, apply)

inline fun PackWriter.addMetadata(json: Json, apply: PackMetadata.() -> Unit) {
    val meta = PackMetadata()
    meta.apply()
    addMetadata(meta, json)
}
