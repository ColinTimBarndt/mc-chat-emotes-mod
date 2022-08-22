@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.serial

import java.io.File
import java.io.IOException
import java.io.OutputStream

class FolderPackWriter(private val folder: File) : PackWriter {
    private var closed = false

    init {
        if (!folder.isDirectory) throw IOException("Not a directory")
    }

    private inline fun ensureOpen() {
        if(closed) throw IOException("Stream closed")
    }

    override fun addFile(name: String): OutputStream {
        ensureOpen()
        val target = folder.resolve(name)
        if (!target.path.startsWith(folder.path)) throw IOException("File not in directory")
        if (target.exists() && !target.isFile) throw IOException("Target is not a file")
        if (!(target.exists())) target.parentFile.mkdirs()
        return target.outputStream()
    }

    override fun close() {
        closed = true
    }

    companion object : PackWriterFactory {
        override val label = "Pack Folder"
        override val fileType = FileType.Folder
        override fun create(file: File) = FolderPackWriter(file)
    }
}