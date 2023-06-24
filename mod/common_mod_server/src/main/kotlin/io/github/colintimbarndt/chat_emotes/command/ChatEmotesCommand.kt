package io.github.colintimbarndt.chat_emotes.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.colintimbarndt.chat_emotes.ComponentFactory
import io.github.colintimbarndt.chat_emotes.common.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.NAMESPACE
import io.github.colintimbarndt.chat_emotes.common.commands.ChatEmotesCommandBase
import io.github.colintimbarndt.chat_emotes.common.permissions.COMMAND_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.RELOAD_COMMAND_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.permissionPredicate
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

private typealias Ctx = CommandContext<CommandSourceStack>

class ChatEmotesCommand(
    private val mod: ChatEmotesServerModBase
) : ChatEmotesCommandBase<CommandSourceStack, Ctx, Component>(
    ComponentFactory,
    CommandAdapter,
) {
    override fun performReloadConfig(): Boolean = mod.reloadConfig()

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val reload = Commands.literal("reload")
            .requires(
                mod.permissionsAdapter.permissionPredicate(
                    RELOAD_COMMAND_PERMISSION
                )
            )
            .executes(::tryReloadConfig)
        dispatcher.register(
            Commands.literal(NAMESPACE)
                .requires(
                    mod.permissionsAdapter.permissionPredicate(
                        COMMAND_PERMISSION
                    )
                )
                .then(reload)
        )
    }
}