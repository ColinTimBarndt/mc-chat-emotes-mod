package io.github.colintimbarndt.chat_emotes.common.permissions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.luckperms.api.LuckPermsProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

/**
 * See [luckperms.net](https://luckperms.net/)
 */
@Serializable
@SerialName("luckperms")
object LuckpermsPermissionsAdapter : PermissionsAdapter {
    private val api = LuckPermsProvider.get()

    override fun CommandSourceStack.hasPermission(name: String): Boolean {
        val player = player
            ?: return true // console / command block
        return player.hasPermission(name)
    }

    override fun ServerPlayer.hasPermission(name: String): Boolean {
        val user = api.userManager.getUser(uuid)
            ?: return false // User not found
        val perms = user.cachedData.permissionData
        return perms.checkPermission(name).asBoolean()
    }
}