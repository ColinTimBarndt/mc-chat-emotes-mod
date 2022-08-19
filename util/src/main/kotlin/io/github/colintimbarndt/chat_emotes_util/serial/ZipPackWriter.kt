@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.serial

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipPackWriter private constructor(
    private val zipFile: ZipOutputStream
) : PackWriter {
    init {
        zipFile.setMethod(ZipOutputStream.DEFLATED)
    }

    override fun addFile(name: String): OutputStream = PackedFile(name)

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

    companion object : PackWriterFactory {
        override val label: String = "Zipped Pack"
        override val fileType: FileType = FileType.Zip
        override fun of(file: File) = of(file.outputStream())
        fun of(stream: OutputStream) = ZipPackWriter(ZipOutputStream(stream))
    }
}