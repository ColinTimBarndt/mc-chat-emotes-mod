package io.github.colintimbarndt.chat_emotes.common.permissions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("minecraft")
object VanillaPermissionsAdapter : PermissionsAdapter {
    override fun CommandSourceStack.hasPermission(name: String) = when (name) {
        RELOAD_COMMAND_PERMISSION -> hasPermission(2)
        COMMAND_PERMISSION -> hasPermission(2)
        else -> SafePermissionsAdapter.run { hasPermission(name) }
    }

    override fun ServerPlayer.hasPermission(name: String) = when (name) {
        RELOAD_COMMAND_PERMISSION -> hasPermissions(2)
        COMMAND_PERMISSION -> hasPermissions(2)
        else -> SafePermissionsAdapter.run { hasPermission(name) }
    }
}