package io.github.colintimbarndt.chat_emotes.common.permissions

import net.minecraft.commands.CommandSourceStack

object VanillaPermissionsAdapter : PermissionsAdapter {
    override fun CommandSourceStack.hasPermission(name: String) = when (name) {
        RELOAD_COMMAND_PERMISSION -> hasPermission(2)
        // Only allows the server console to execute this subcommand by default,
        // as it directly modifies the host file system.
        // This can be changed by using a supported permissions mod or plugin
        EXPORT_COMMAND_PERMISSION -> !isPlayer && hasPermission(4)
        COMMAND_PERMISSION -> hasPermission(2)
        else -> name.startsWith(EMOTES_PERMISSION)
                && (name.length == EMOTES_PERMISSION.length || name[EMOTES_PERMISSION.length] == '.')
    }
}