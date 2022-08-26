package io.github.colintimbarndt.chat_emotes.common.permissions

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

interface PermissionsAdapter {
    fun CommandSourceStack.hasPermission(name: String): Boolean
    fun ServerPlayer.hasPermission(name: String): Boolean
}