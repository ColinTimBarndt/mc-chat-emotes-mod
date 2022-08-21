package io.github.colintimbarndt.chat_emotes_util

interface DefaultedEnum<T> {
    val default: T
    fun values(): Array<T>
}