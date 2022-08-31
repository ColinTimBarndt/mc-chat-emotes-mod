package io.github.colintimbarndt.chat_emotes.common.permissions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

/**
 * Only emotes are permitted, every other permission is denied
 */
@Serializable
@SerialName("safe")
object SafePermissionsAdapter : PermissionsAdapter {
    override fun CommandSourceStack.hasPermission(name: String) = isEmotesPermission(name)

    override fun ServerPlayer.hasPermission(name: String) = isEmotesPermission(name)

    private fun isEmotesPermission(name: String): Boolean =
        name.startsWith(EMOTES_PERMISSION)
                && (name.length == EMOTES_PERMISSION.length || name[EMOTES_PERMISSION.length] == '.')
}