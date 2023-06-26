package io.github.colintimbarndt.chat_emotes.common.mod

import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.common.mod.data.EmoteDataLoaderBase
import io.github.colintimbarndt.chat_emotes.common.permissions.NoPermissionsAdapter
import io.github.colintimbarndt.chat_emotes.common.permissions.PermissionsAdapter
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

abstract class ChatEmotesServerModBase {
    private var innerConfig: ChatEmotesConfig = ChatEmotesConfig.DEFAULT
    private var innerPermissionsAdapter: PermissionsAdapter<ServerPlayer, CommandSourceStack> =
        NoPermissionsAdapter

    abstract val emoteDataLoader: EmoteDataLoaderBase

    @Suppress("LeakingThis") // Only accessed after construction
    val emoteDecorator = EmoteDecorator(this)


    @get:Synchronized
    @set:Synchronized
    var config: ChatEmotesConfig = innerConfig

    @get:Synchronized
    @set:Synchronized
    var permissionsAdapter: PermissionsAdapter<ServerPlayer, CommandSourceStack> =
        innerPermissionsAdapter
}