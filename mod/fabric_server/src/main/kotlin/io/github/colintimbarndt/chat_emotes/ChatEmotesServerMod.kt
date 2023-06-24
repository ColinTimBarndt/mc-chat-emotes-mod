package io.github.colintimbarndt.chat_emotes

import io.github.colintimbarndt.chat_emotes.common.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.data.EmoteDataLoader
import io.github.colintimbarndt.chat_emotes.permissions.VanillaPermissionsAdapter
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.packs.PackType
import java.nio.file.Path

object ChatEmotesServerMod :
    ChatEmotesServerModBase(),
    DedicatedServerModInitializer {
    override lateinit var configPath: Path private set
    override lateinit var config: ChatEmotesConfig
    override val emoteDataLoader = EmoteDataLoader
    override val emoteDecorator = EmoteDecorator

    override fun onInitializeServer() {
        val loader = FabricLoader.getInstance()
        val confDir = loader.configDir.resolve(MOD_ID)
        val configDirFile = confDir.toFile()
        configPath = confDir
        if (!(configDirFile.isDirectory || configDirFile.mkdirs())) {
            LOGGER.error("Failed to create config directory")
            config = ChatEmotesConfig()
        } else {
            reloadConfig()
        }
        CommandRegistrationCallback.EVENT.register(::onRegisterCommands)
        ServerMessageDecoratorEvent.EVENT.register(
            ServerMessageDecoratorEvent.CONTENT_PHASE,
            EmoteDecorator
        )
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(emoteDataLoader)

        permissionsAdapter = VanillaPermissionsAdapter
    }
}