package io.github.colintimbarndt.chat_emotes.common.util

import org.apache.commons.io.ByteOrderMark
import org.apache.commons.io.input.BOMInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object BomAwareReader {
    private val BO_MARKS = arrayOf(
        ByteOrderMark.UTF_16LE,
        ByteOrderMark.UTF_16BE,
        ByteOrderMark.UTF_8
    )

    @Throws(IOException::class)
    fun create(delegate: InputStream?): InputStreamReader {
        val bomStream = BOMInputStream(delegate, *BO_MARKS)
        val charset = if (bomStream.hasBOM()) {
            when (bomStream.bomCharsetName) {
                "UTF-16LE" -> StandardCharsets.UTF_16LE
                "UTF-16BE" -> StandardCharsets.UTF_16BE
                else -> StandardCharsets.UTF_8
            }
        } else {
            StandardCharsets.UTF_8
        }
        return InputStreamReader(bomStream, charset)
    }

    @Throws(IOException::class)
    fun createBuffered(delegate: InputStream?): BufferedReader {
        return BufferedReader(create(delegate))
    }

    @Throws(IOException::class)
    fun createBuffered(delegate: InputStream?, sz: Int): BufferedReader {
        return BufferedReader(create(delegate), sz)
    }
}