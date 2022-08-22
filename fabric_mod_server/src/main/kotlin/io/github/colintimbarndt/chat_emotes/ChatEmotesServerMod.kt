package io.github.colintimbarndt.chat_emotes

import com.mojang.brigadier.CommandDispatcher
import io.github.colintimbarndt.chat_emotes.command.ChatEmotesCommand
import io.github.colintimbarndt.chat_emotes.common.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.common.Registries
import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.data.EmoteDataLoader
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.server.packs.PackType
import java.nio.file.Path

object ChatEmotesServerMod : ChatEmotesServerModBase(), DedicatedServerModInitializer {
    override lateinit var configPath: Path private set
    override lateinit var config: ChatEmotesConfig
    override lateinit var registries: Registries private set
    override val emoteDataLoader = EmoteDataLoader
    override val emoteDecorator = EmoteDecorator
    lateinit var modMetadata: ModMetadata private set

    override fun onInitializeServer() {
        registries = Registries()

        val loader = FabricLoader.getInstance()
        modMetadata = loader.getModContainer(MOD_ID).orElseThrow().metadata
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
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, EmoteDecorator)
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(emoteDataLoader)
    }

    private fun onRegisterCommands(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        reg: CommandBuildContext,
        env: CommandSelection
    ) {
        if (env.includeDedicated) {
            ChatEmotesCommand.register(dispatcher)
        }
    }
}