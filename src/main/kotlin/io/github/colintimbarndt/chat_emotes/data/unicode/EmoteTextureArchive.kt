package io.github.colintimbarndt.chat_emotes.data.unicode

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class EmoteTextureArchive(file: File) : ZipFile(file, OPEN_READ) {
    private val textures = TreeMap<String, ZipEntry>()

    init {
        val entries = entries().asIterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            if (!FILENAME_PATTERN.matcher(entry.name).matches()) continue
            val seq = StringBuilder(32)
            val entryName = entry.name
            FILENAME_SEPARATOR_PATTERN.splitAsStream(entryName.substring(0, entryName.length - 4))
                .forEach { s: String ->
                    val cp = s.toInt(16)
                    if (Character.isBmpCodePoint(cp)) {
                        seq.append(cp.toChar())
                    } else {
                        seq.append(Character.highSurrogate(cp))
                        seq.append(Character.lowSurrogate(cp))
                    }
                }
            textures[seq.toString()] = entry
        }
    }

    @Throws(IOException::class)
    fun getTextureAsStream(seq: String): InputStream? {
        val entry = textures[seq] ?: return null
        return getInputStream(entry)
    }

    companion object {
        val FILENAME_PATTERN: Pattern = Pattern.compile(
            "^[\\da-z]{1,6}((\\s+|[_-])[\\da-z]{1,6})*\\.png$",
            Pattern.CASE_INSENSITIVE
        )
        val FILENAME_SEPARATOR_PATTERN: Pattern = Pattern.compile("\\s+|[_-]")
    }
}