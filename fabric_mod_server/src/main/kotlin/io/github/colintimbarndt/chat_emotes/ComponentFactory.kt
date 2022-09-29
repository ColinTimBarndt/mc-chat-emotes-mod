package io.github.colintimbarndt.chat_emotes

import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentBuilder
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentFactory
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.LiteralContents
import java.util.*

object ComponentFactory : AbstractComponentFactory<Component>() {
    override fun literal(text: String): AbstractComponentBuilder<Component> {
        return ComponentBuilder(Component.literal(text))
    }

    override fun text(text: String): Component = Component.literal(text)

    override fun translatable(
        key: String,
        with: List<Component>
    ): AbstractComponentBuilder<Component> {
        return ComponentBuilder(Component.translatable(key, *with.toTypedArray()))
    }

    override val Component.siblingComponents: List<Component>
        get() = siblings

    override fun Component.literalContent(): Optional<String> {
        return if (contents is LiteralContents) Optional.of((contents as LiteralContents).text())
        else Optional.empty()
    }

    override fun <T> Component.visit(visitor: (text: String) -> Optional<T>): Optional<T> {
        return visit { visitor(it) }
    }
}