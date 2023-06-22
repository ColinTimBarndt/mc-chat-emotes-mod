package io.github.colintimbarndt.chat_emotes.common.util

import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentBuilder
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentFactory
import io.github.colintimbarndt.chat_emotes.common.util.StringBuilderExt.plusAssign
import java.util.*

@Suppress("NOTHING_TO_INLINE")
object ComponentUtils {
    const val FALLBACK_TRANSLATION_KEY = "%1\$s%784014\$s"

    inline fun <Component> AbstractComponentFactory<Component>.fallback(
        fallback: Component,
        other: Component
    ): AbstractComponentBuilder<Component> {
        return translatable(FALLBACK_TRANSLATION_KEY, listOf(fallback, other))
    }

    fun <Component> AbstractComponentFactory<Component>.fallbackTranslatable(
        key: String
    ): AbstractComponentBuilder<Component> {
        return this.fallbackTranslatable(key, Collections.emptyList())
    }

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
        return fallback(text(builder.toString()), inner)
    }
}