package io.github.colintimbarndt.chat_emotes.util

import io.github.colintimbarndt.chat_emotes.util.StringBuilderExt.plusAssign
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.util.*

@Suppress("NOTHING_TO_INLINE")
object ComponentUtils {
    inline fun fallback(fallback: Component, other: Component): MutableComponent {
        return Component.translatable("%1\$s%784014\$s", fallback, other)
    }

    fun fallbackTranslatable(key: String, vararg args: Any?): MutableComponent {
        val inner = Component.translatable(key, *args)
        val builder = StringBuilder()
        inner.visit {
            builder += it
            Optional.empty<Any>()
        }
        return fallback(Component.literal(builder.toString()), inner)
    }

    inline operator fun MutableComponent.plusAssign(raw: String) {
        append(raw)
    }

    inline operator fun MutableComponent.plusAssign(c: Component) {
        append(c)
    }

    inline operator fun MutableComponent.plusAssign(cs: List<Component>) {
        for (c in cs) this += c
    }

    inline fun MutableComponent.withStyle(base: Style, modify: Style.() -> Style): MutableComponent =
        setStyle(base.modify())
}