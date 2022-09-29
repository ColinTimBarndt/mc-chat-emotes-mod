package io.github.colintimbarndt.chat_emotes

import io.github.colintimbarndt.chat_emotes.common.EmoteDecoratorBase
import net.minecraft.network.chat.ChatDecorator
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.util.concurrent.CompletableFuture

object EmoteDecorator :
    EmoteDecoratorBase<ServerPlayer, Component>(
        ComponentFactory
    ), ChatDecorator {
    override val emoteDataLoader
        get() = ChatEmotesServerMod.emoteDataLoader

    override val config
        get() = ChatEmotesServerMod.config

    override val permissionsAdapter
        get() = ChatEmotesServerMod.permissionsAdapter

    override fun decorate(
        serverPlayer: ServerPlayer?,
        component: Component
    ): CompletableFuture<Component> =
        CompletableFuture.completedFuture(decorateSync(serverPlayer, component))
}
