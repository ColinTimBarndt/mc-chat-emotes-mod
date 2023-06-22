package io.github.colintimbarndt.chat_emotes_util.serial

import java.io.Closeable
import java.io.InputStream

class CsvReader<out T> private constructor(
    private val backingCloseable: Closeable,
    private val lines: Iterator<String>,
    private val header: Map<String, Int>,
    private val revive: (CsvEntry<T>) -> T,
) : Sequence<T> {
    var iteratorCalled = false
    override fun iterator(): Iterator<T> {
        if (iteratorCalled) throw IllegalStateException("Can only iterate once")
        iteratorCalled = true
        return CsvIterator()
    }
    private lateinit var line: List<String>

    private inner class CsvIterator : Iterator<T> {
        private var hasNext = lines.hasNext()
        override fun hasNext() = hasNext
        override fun next(): T {
            line = lines.next().split(',')
            if (!lines.hasNext()) backingCloseable.use {
                hasNext = false
                return revive(CsvEntry(this@CsvReader))
            }
            try {
                return revive(CsvEntry(this@CsvReader))
            } catch (e: Throwable) {
                hasNext = false
                backingCloseable.close()
                throw e
            }
        }
    }

    @JvmInline
    value class CsvEntry<out T>(private val parser: CsvReader<T>) {
        operator fun get(column: String): String {
            val idx = parser.header[column] ?: -1
            if (idx < 0) throw IllegalArgumentException("column '$column' does not exist")
            if (idx >= parser.line.size) return ""
            return parser.line[idx]
        }
    }

    companion object {
        fun <T> parse(stream: InputStream, revive: (CsvEntry<T>) -> T): CsvReader<T> {
            val reader = stream.bufferedReader()
            val headerRaw = reader.readLine().split(',')
            val header = headerRaw.asSequence().mapIndexed { i, h -> h to i }.toMap()
            return CsvReader(reader, reader.lines().iterator(), header, revive)
        }
    }
}