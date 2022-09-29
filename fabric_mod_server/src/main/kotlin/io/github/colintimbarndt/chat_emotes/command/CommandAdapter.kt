package io.github.colintimbarndt.chat_emotes.command

import com.mojang.brigadier.context.CommandContext
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractCommandAdapter
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component

object CommandAdapter : AbstractCommandAdapter<CommandSourceStack, CommandContext<CommandSourceStack>, Component>() {
    override fun CommandContext<CommandSourceStack>.sendSuccess(
        message: Component,
        broadcast: Boolean
    ) = source.sendSuccess(message, broadcast)

    override fun CommandContext<CommandSourceStack>.sendFailure(message: Component): Unit =
        source.sendFailure(message)

    override val CommandContext<CommandSourceStack>.commandSource: CommandSourceStack
        get() = source
}