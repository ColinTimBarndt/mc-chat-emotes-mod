package io.github.colintimbarndt.chat_emotes.forge

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.eventbus.api.Event
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

class ServerMessageDecoratorEvent(
    val sender: ServerPlayer?,
    var message: Component,
) : Event() {
    companion object {
        fun decorate(sender: ServerPlayer?, message: Component): Component {
            val event = ServerMessageDecoratorEvent(sender, message)
            FORGE_BUS.post(event)
            return event.message
        }
    }
}