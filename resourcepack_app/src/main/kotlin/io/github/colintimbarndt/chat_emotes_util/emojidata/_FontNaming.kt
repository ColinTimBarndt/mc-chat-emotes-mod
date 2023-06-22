@file:JvmName("FontNamingKt")

package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.Labeled
import kotlin.reflect.KClass

interface FontNaming<G : Any> {
    val groupKeyType: KClass<G>
    fun getGroupKey(data: FlatEmojiData): G
    fun getFontName(key: G): String

    companion object {
        val values: MutableList<FontNamingFactory> = mutableListOf(
            WithName,
            NumberedCategories,
            NamedCategories,
        )
        val default: FontNamingFactory = WithName
    }
}

interface FontNamingFactory : Labeled {
    val example: String
    fun create(template: String): FontNaming<*>
}

private fun String.applyTemplate(value: String): String {
    var idx = -1
    do {
        idx = indexOf('{', ++idx)
        if (idx == lastIndex) return this
        if (value[idx + 1] == '}') {
            val before = substring(0 until idx)
            val after = substring(idx + 2)
            return "$before$value$after"
        }
    } while (idx >= 0)
    return this
}

private class WithName(
    private val format: String
) : FontNaming<Unit> {
    companion object : FontNamingFactory {
        override val label = "with name"
        override val example = "emoji"
        override fun create(template: String): FontNaming<Unit> = WithName(template)
    }
    override val groupKeyType = Unit::class
    override fun getGroupKey(data: FlatEmojiData) {}
    override fun getFontName(key: Unit) = format
}

private class NumberedCategories private constructor(
    private val format: String
): FontNaming<String> {
    companion object : FontNamingFactory {
        override val label = "grouped by numbered category"
        override val example = "emoji_{}"
        override fun create(template: String): FontNaming<String> = NumberedCategories(template)
    }
    private var count = 0
    override val groupKeyType = String::class
    override fun getGroupKey(data: FlatEmojiData) = data.category
    override fun getFontName(key: String) = format.applyTemplate(count++.toString())
}

private class NamedCategories private constructor(
    private val format: String
): FontNaming<String> {
    companion object : FontNamingFactory {
        override val label = "grouped by named category"
        override val example = "emoji_{}"
        override fun create(template: String): FontNaming<String> = NamedCategories(template)
    }
    override val groupKeyType = String::class
    override fun getGroupKey(data: FlatEmojiData) = data.category
    override fun getFontName(key: String): String {
        val sb = StringBuilder(key.length + 4)
        for (c in key) {
            when (val cl = c.lowercaseChar()) {
                in 'a'..'z' -> sb.append(cl)
                ' ' -> sb.append('_')
                '&' -> sb.append("and")
            }
        }
        return format.applyTemplate(sb.toString())
    }
}