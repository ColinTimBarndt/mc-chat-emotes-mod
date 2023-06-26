package io.github.colintimbarndt.chat_emotes.common.mod.permissions

import io.github.colintimbarndt.chat_emotes.common.permissions.COMMAND_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.EMOTES_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.PermissionsAdapter
import io.github.colintimbarndt.chat_emotes.common.permissions.RELOAD_COMMAND_PERMISSION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("minecraft")
object VanillaPermissionsAdapter :
    PermissionsAdapter<ServerPlayer, CommandSourceStack> {
    override fun contextHasPermission(
        ctx: CommandSourceStack,
        name: String
    ) = when (name) {
        RELOAD_COMMAND_PERMISSION -> ctx.hasPermission(2)
        COMMAND_PERMISSION -> ctx.hasPermission(2)
        else -> name.startsWith(EMOTES_PERMISSION)
                && (name.length == EMOTES_PERMISSION.length || name[EMOTES_PERMISSION.length] == '.')
    }

    override fun playerHasPermission(p: ServerPlayer, name: String) = when (name) {
        RELOAD_COMMAND_PERMISSION -> p.hasPermissions(2)
        COMMAND_PERMISSION -> p.hasPermissions(2)
        else -> name.startsWith(EMOTES_PERMISSION)
                && (name.length == EMOTES_PERMISSION.length || name[EMOTES_PERMISSION.length] == '.')
    }
}