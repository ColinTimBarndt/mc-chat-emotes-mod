package io.github.colintimbarndt.chat_emotes.util

@Suppress("NOTHING_TO_INLINE")
object StringBuilderExt {
    inline operator fun StringBuilder.plusAssign(v: String) { append(v) }
    inline operator fun StringBuilder.plusAssign(v: Char) { append(v) }
}