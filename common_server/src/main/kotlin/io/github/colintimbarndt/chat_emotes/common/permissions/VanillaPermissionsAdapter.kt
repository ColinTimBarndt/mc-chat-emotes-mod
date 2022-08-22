package io.github.colintimbarndt.chat_emotes.common.permissions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.commands.CommandSourceStack

@Serializable
@SerialName("minecraft")
object VanillaPermissionsAdapter : PermissionsAdapter {
    override fun CommandSourceStack.hasPermission(name: String) = when (name) {
        RELOAD_COMMAND_PERMISSION -> hasPermission(2)
        COMMAND_PERMISSION -> hasPermission(2)
        else -> name.startsWith(EMOTES_PERMISSION)
                && (name.length == EMOTES_PERMISSION.length || name[EMOTES_PERMISSION.length] == '.')
    }
}