package io.github.colintimbarndt.chat_emotes.common.mod

import io.github.colintimbarndt.chat_emotes.common.EmoteDecoratorBase
import net.minecraft.network.chat.ChatDecorator
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.util.concurrent.CompletableFuture

class EmoteDecorator(
    private val mod: ChatEmotesServerModBase
) : EmoteDecoratorBase<ServerPlayer, Component>(ComponentFactory), ChatDecorator {

    override val emoteData
        get() = mod.emoteDataLoader

    override val config
        get() = mod.config

    override val permissionsAdapter
        get() = mod.permissionsAdapter

    override fun decorate(
        serverPlayer: ServerPlayer?,
        component: Component
    ): CompletableFuture<Component> =
        CompletableFuture.completedFuture(decorateSync(serverPlayer, component))
}