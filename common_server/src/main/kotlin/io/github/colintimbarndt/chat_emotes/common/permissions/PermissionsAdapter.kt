package io.github.colintimbarndt.chat_emotes.common.permissions

import net.minecraft.commands.CommandSourceStack

interface PermissionsAdapter {
    fun CommandSourceStack.hasPermission(name: String): Boolean
}