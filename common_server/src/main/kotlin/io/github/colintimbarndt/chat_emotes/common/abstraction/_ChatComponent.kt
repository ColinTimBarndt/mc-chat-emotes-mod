@file:JvmName("ChatComponentKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes.common.abstraction

import io.github.colintimbarndt.chat_emotes.common.data.ResourceLocation
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class AbstractComponentFactory<Component> {
    open fun empty() = literal("")
    abstract fun literal(text: String): AbstractComponentBuilder<Component>

    fun translatable(key: String, with: List<Component>) = translatable(key, with, null)
    abstract fun translatable(
        key: String,
        with: List<Component>,
        fallback: String?,
    ): AbstractComponentBuilder<Component>

    open fun text(text: String) = literal(text).build()

    // Component extension
    abstract fun <T> Component.visit(visitor: (text: String) -> Optional<T>): Optional<T>
    abstract fun Component.literalContent(): Optional<String>
    abstract val Component.siblingComponents: List<Component>
}

abstract class AbstractImmutableComponentBuilder<Component> {
    abstract fun append(next: Component): AbstractImmutableComponentBuilder<Component>
    abstract fun append(text: String): AbstractComponentBuilder<Component>
    abstract fun build(): Component
    abstract fun copied(): AbstractComponentBuilder<Component>

    inline operator fun plus(raw: String) = append(raw)

    inline operator fun plus(c: Component) = append(c)

    inline fun append(cs: Iterable<Component>): AbstractImmutableComponentBuilder<Component> {
        var t: AbstractImmutableComponentBuilder<Component> = this
        for (c in cs) t += c
        return t
    }
}

abstract class AbstractComponentBuilder<Component> :
    AbstractImmutableComponentBuilder<Component>() {
    abstract fun color(color: ChatColor): AbstractComponentBuilder<Component>
    abstract fun bold(bold: Boolean): AbstractComponentBuilder<Component>
    abstract fun italic(italic: Boolean): AbstractComponentBuilder<Component>
    abstract fun underlined(underline: Boolean): AbstractComponentBuilder<Component>
    abstract fun insertion(insertion: String): AbstractComponentBuilder<Component>
    abstract fun font(font: ResourceLocation): AbstractComponentBuilder<Component>

    abstract fun onHoverShowText(text: Component)

    // Helper functions

    @OptIn(ExperimentalContracts::class)
    inline fun onHover(block: HoverContext<Component>.() -> Unit): AbstractComponentBuilder<Component> {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        HoverContext(this).block()
        return this
    }
}

@JvmInline
value class HoverContext<Component>(
    @PublishedApi internal val builder: AbstractComponentBuilder<Component>
) {
    inline fun showText(text: Component) = builder.onHoverShowText(text)
}

enum class ChatColor {
    BLACK,
    DARK_BLUE,
    DARK_GREEN,
    DARK_AQUA,
    DARK_RED,
    DARK_PURPLE,
    GOLD,
    GRAY,
    DARK_GRAY,
    BLUE,
    GREEN,
    AQUA,
    RED,
    LIGHT_PURPLE,
    YELLOW,
    WHITE,
}