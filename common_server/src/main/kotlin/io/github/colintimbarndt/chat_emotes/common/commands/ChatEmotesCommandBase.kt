package io.github.colintimbarndt.chat_emotes.common.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import io.github.colintimbarndt.chat_emotes.common.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.NAMESPACE
import io.github.colintimbarndt.chat_emotes.common.permissions.COMMAND_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.RELOAD_COMMAND_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.permissionPredicate
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.fallbackTranslatable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import java.util.*

abstract class ChatEmotesCommandBase {
    protected abstract val serverMod: ChatEmotesServerModBase

    /**
     * Gets the icon used when generating a resource pack.
     * This is a path to the mod icon file in the mod classloader
     */
    protected abstract fun getIcon(): Optional<String>

    private fun tryReloadConfig(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        return if (serverMod.reloadConfig()) {
            source.sendSuccess(
                fallbackTranslatable("commands.chat_emotes.reload.success"),
                true
            )
            1
        } else {
            source.sendFailure(
                fallbackTranslatable("commands.chat_emotes.reload.failure")
            )
            0
        }
    }

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