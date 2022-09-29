package io.github.colintimbarndt.chat_emotes.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.colintimbarndt.chat_emotes.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.ComponentFactory
import io.github.colintimbarndt.chat_emotes.common.NAMESPACE
import io.github.colintimbarndt.chat_emotes.common.commands.ChatEmotesCommandBase
import io.github.colintimbarndt.chat_emotes.common.permissions.COMMAND_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.RELOAD_COMMAND_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.permissionPredicate
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object ChatEmotesCommand : ChatEmotesCommandBase<CommandSourceStack, CommandContext<CommandSourceStack>, Component>(
    ComponentFactory,
    CommandAdapter,
) {
    override val serverMod get() = ChatEmotesServerMod

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val reload = Commands.literal("reload")
            .requires(serverMod.permissionsAdapter.permissionPredicate(RELOAD_COMMAND_PERMISSION))
            .executes(::tryReloadConfig)
        dispatcher.register(
            Commands.literal(NAMESPACE)
                .requires(serverMod.permissionsAdapter.permissionPredicate(COMMAND_PERMISSION))
                .then(reload)
        )
    }
}
