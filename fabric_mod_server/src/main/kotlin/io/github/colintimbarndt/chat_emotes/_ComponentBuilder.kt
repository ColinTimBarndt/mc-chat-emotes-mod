@file:JvmName("ComponentBuilderKt")

package io.github.colintimbarndt.chat_emotes

import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentBuilder
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractImmutableComponentBuilder
import io.github.colintimbarndt.chat_emotes.common.abstraction.ChatColor
import io.github.colintimbarndt.chat_emotes.common.abstraction.ChatColor.*
import io.github.colintimbarndt.chat_emotes.common.data.ResourceLocation
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

abstract class BasicComponentBuilder internal constructor(
    protected val delegate: MutableComponent,
) : AbstractComponentBuilder<Component>() {
    private var built = false

    protected fun ensureNotBuilt() {
        if (built) throw IllegalStateException("already built")
    }

    protected fun setBuilt() {
        built = true
    }

    override fun color(color: ChatColor): AbstractComponentBuilder<Component> {
        ensureNotBuilt()
        delegate.style = delegate.style.withColor(when (color) {
            BLACK -> ChatFormatting.BLACK
            DARK_BLUE -> ChatFormatting.DARK_BLUE
            DARK_GREEN -> ChatFormatting.DARK_GREEN
            DARK_AQUA -> ChatFormatting.DARK_AQUA
            DARK_RED -> ChatFormatting.DARK_RED
            DARK_PURPLE -> ChatFormatting.DARK_PURPLE
            GOLD -> ChatFormatting.GOLD
            GRAY -> ChatFormatting.GRAY
            DARK_GRAY -> ChatFormatting.DARK_GRAY
            BLUE -> ChatFormatting.BLUE
            GREEN -> ChatFormatting.GREEN
            AQUA -> ChatFormatting.AQUA
            RED -> ChatFormatting.RED
            LIGHT_PURPLE -> ChatFormatting.LIGHT_PURPLE
            YELLOW -> ChatFormatting.YELLOW
            WHITE -> ChatFormatting.WHITE
        })
        return this
    }

    override fun bold(bold: Boolean): AbstractComponentBuilder<Component> {
        ensureNotBuilt()
        delegate.style = delegate.style.withBold(bold)
        return this
    }

    override fun italic(italic: Boolean): AbstractComponentBuilder<Component> {
        ensureNotBuilt()
        delegate.style = delegate.style.withItalic(italic)
        return this
    }

    override fun underlined(underline: Boolean): AbstractComponentBuilder<Component> {
        ensureNotBuilt()
        delegate.style = delegate.style.withUnderlined(underline)
        return this
    }

    override fun insertion(insertion: String): AbstractComponentBuilder<Component> {
        ensureNotBuilt()
        delegate.style = delegate.style.withInsertion(insertion)
        return this
    }

    override fun font(font: ResourceLocation): AbstractComponentBuilder<Component> {
        ensureNotBuilt()
        delegate.style = delegate.style.withFont(net.minecraft.resources.ResourceLocation(font.namespace, font.path))
        return this
    }

    override fun onHoverShowText(text: Component) {
        ensureNotBuilt()
        delegate.style = delegate.style.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                text
            )
        )
    }
}

class ComponentBuilder internal constructor(
    delegate: MutableComponent,
) : BasicComponentBuilder(delegate) {
    override fun append(text: String): AbstractComponentBuilder<Component> {
        ensureNotBuilt()
        val comp = Component.literal(text)
        delegate.append(comp)
        return SiblingComponentBuilder(comp)
    }

    override fun append(next: Component): AbstractImmutableComponentBuilder<Component> {
        ensureNotBuilt()
        return SiblingImmutableComponentBuilder(next)
    }

    override fun build(): Component {
        ensureNotBuilt()
        setBuilt()
        return delegate
    }

    override fun copied(): AbstractComponentBuilder<Component> {
        return ComponentBuilder(delegate.copy())
    }

    private inner class SiblingImmutableComponentBuilder(
        private val component: Component
    ) : AbstractImmutableComponentBuilder<Component>() {
        override fun append(next: Component): AbstractImmutableComponentBuilder<Component> {
            ensureNotBuilt()
            delegate.append(component)
            return SiblingImmutableComponentBuilder(next)
        }

        override fun append(text: String): AbstractComponentBuilder<Component> {
            ensureNotBuilt()
            delegate.append(component)
            return this@ComponentBuilder.append(text)
        }

        override fun build(): Component {
            ensureNotBuilt()
            delegate.append(component)
            return this@ComponentBuilder.build()
        }

        override fun copied(): AbstractComponentBuilder<Component> {
            val copy = component.copy()
            delegate.append(copy)
            return SiblingComponentBuilder(copy)
        }
    }

    private inner class SiblingComponentBuilder(component: MutableComponent) : BasicComponentBuilder(component) {
        override fun append(next: Component): AbstractImmutableComponentBuilder<Component> {
            ensureNotBuilt()
            return SiblingImmutableComponentBuilder(next)
        }

        override fun append(text: String) = this@ComponentBuilder.append(text)

        override fun build() = this@ComponentBuilder.build()

        override fun copied(): AbstractComponentBuilder<Component> {
            return ComponentBuilder(delegate.copy())
        }
    }
}