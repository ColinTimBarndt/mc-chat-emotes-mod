package io.github.colintimbarndt.chat_emotes.common.util

import com.google.gson.stream.JsonWriter
import com.mojang.bridge.game.PackType
import net.minecraft.SharedConstants
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("NOTHING_TO_INLINE")
class PackWriter(file: File, packType: PackType) : Closeable, AutoCloseable {
    @PublishedApi
    internal val zipStream: ZipOutputStream
    val packType: PackType

    init {
        if (!(file.exists() || file.createNewFile())) {
            throw IOException("Unable to create file")
        }
        zipStream = ZipOutputStream(FileOutputStream(file))
        zipStream.setMethod(ZipOutputStream.DEFLATED)
        this.packType = packType
    }

    @Throws(IOException::class)
    fun write(
        name: String,
        inStream: InputStream
    ) {
        zipStream.putNextEntry(ZipEntry(name))
        val stream = PackEntryOutputStream()
        stream.use(inStream::transferTo)
    }

    @Throws(IOException::class)
    inline fun write(
        name: String,
        write: (OutputStream) -> Unit
    ) {
        zipStream.putNextEntry(ZipEntry(name))
        val stream = PackEntryOutputStream()
        write(stream)
        stream.close()
    }

    @Throws(IOException::class)
    inline fun write(meta: PackMeta) {
        if (meta.version == -1)
            meta version SharedConstants.getCurrentVersion().getPackVersion(packType)
        write("pack.mcmeta", meta::write)
    }

    @Throws(IOException::class)
    fun write(
        loc: ResourceLocation,
        writer: (OutputStream) -> Unit
    ) {
        val path = (if (packType == PackType.DATA) "data" else "assets") +
                "/${loc.namespace}/${loc.path}"
        write(path, writer)
    }

    @Throws(IOException::class)
    override fun close() {
        zipStream.use {
            it.finish()
            it.flush()
        }
    }

    class PackMeta {
        var description: Component = Component.empty()
        var version = -1

        inline infix fun version(v: Int): PackMeta {
            version = v
            return this
        }

        inline infix fun description(description: Component): PackMeta {
            this.description = description
            return this
        }

        inline infix fun description(description: String): PackMeta {
            this.description = Component.nullToEmpty(description)
            return this
        }

        @Throws(IOException::class)
        fun write(out: OutputStream) {
            JsonWriter(OutputStreamWriter(out)).use {
                it.setIndent("  ")
                it.beginObject()
                it.name("pack").beginObject()
                it.name("pack_format").value(version)
                it.name("description")
                it.jsonValue(Component.Serializer.toJson(description))
                it.endObject()
                it.endObject()
            }
        }
    }

    inner class PackEntryOutputStream : OutputStream() {
        private var closed = false

        @Throws(IOException::class)
        override fun write(i: Int) {
            if (closed) {
                throw IOException("Stream closed")
            }
            zipStream.write(i)
        }

        @Throws(IOException::class)
        override fun close() {
            if (!closed) {
                closed = true
                zipStream.closeEntry()
            }
        }
    }
}