package io.github.colintimbarndt.chat_emotes.common.util

import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentBuilder
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentFactory
import io.github.colintimbarndt.chat_emotes.common.util.StringBuilderExt.plusAssign
import java.util.*

@Suppress("NOTHING_TO_INLINE")
object ComponentUtils {
    const val EMOTE_TRANSLATION_KEY = "chat.emote"

    inline fun <Component> AbstractComponentFactory<Component>.fallback(
        fallback: Component,
        other: Component,
        fallbackKey: String
    ) = translatable(fallbackKey, listOf(other, fallback), "%2\$s")

    inline fun <Component> AbstractComponentFactory<Component>.fallbackTranslatable(
        key: String
    ) = fallbackTranslatable(key, Collections.emptyList())

    fun <Component> AbstractComponentFactory<Component>.fallbackTranslatable(
        key: String,
        args: List<Component>
    ): AbstractComponentBuilder<Component> {
        val inner = translatable(key, args).build()
        val builder = StringBuilder()
        inner.visit {
            builder += it
            Optional.empty<Nothing>()
        }
        val fallback = text(builder.toString())
        val argsExt = if (args.isNotEmpty()) ArrayList<Component>(args.size + 1).apply {
            addAll(args)
            add(fallback)
        } else {
            listOf(fallback)
        }
        println("Fallback for $key: $builder")
        return translatable(key, argsExt, "%${argsExt.size}\$s")
    }
}