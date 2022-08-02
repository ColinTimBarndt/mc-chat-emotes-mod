package io.github.colintimbarndt.chat_emotes

import com.google.gson.JsonParseException
import com.mojang.brigadier.CommandDispatcher
import com.mojang.serialization.Lifecycle
import io.github.colintimbarndt.chat_emotes.commands.ChatEmotesCommand
import io.github.colintimbarndt.chat_emotes.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.data.EmoteDataLoader
import io.github.colintimbarndt.chat_emotes.data.EmoteDataSerializer
import io.github.colintimbarndt.chat_emotes.data.unicode.UnicodeEmoteData
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path

class ChatEmotesMod : DedicatedServerModInitializer, ClientModInitializer {
    private fun onInitialize(loader: FabricLoader) {
        modMetadata = loader.getModContainer(MOD_ID).orElseThrow().metadata
    }

    override fun onInitializeServer() {
        val loader = FabricLoader.getInstance()
        onInitialize(loader)
        val confDir = loader.configDir.resolve(MOD_ID)
        val configDirFile = confDir.toFile()
        configDir = confDir
        if (!(configDirFile.isDirectory || configDirFile.mkdirs())) {
            LOGGER.error("Failed to create config directory")
            config = ChatEmotesConfig()
        } else {
            reloadConfig()
        }
        CommandRegistrationCallback.EVENT.register(::onRegisterCommands)
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, EmoteDecorator.EMOTES)
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(EMOTE_DATA_LOADER)
    }

    override fun onInitializeClient() {
        val loader = FabricLoader.getInstance()
        onInitialize(loader)
        // TODO: implement client
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

    companion object {
        const val MOD_ID = "chat_emotes"
        @JvmField
		val LOGGER = LoggerFactory.getLogger("Chat Emotes")
        @JvmStatic
		var config: ChatEmotesConfig? = null
            private set
        @JvmStatic
		var configDir: Path? = null
            private set
        @JvmStatic
		var modMetadata: ModMetadata? = null
            private set
        val EMOTE_DATA_SERIALIZER_REGISTRY: ResourceKey<Registry<EmoteDataSerializer<*>>> =
            ResourceKey.createRegistryKey(ResourceLocation(MOD_ID, "emote_data_serializer"))

        @JvmField
		val EMOTE_DATA_SERIALIZER: Registry<EmoteDataSerializer<*>>
        @JvmField
		val EMOTE_DATA_LOADER = EmoteDataLoader()

        init {
            EMOTE_DATA_SERIALIZER = MappedRegistry(EMOTE_DATA_SERIALIZER_REGISTRY, Lifecycle.experimental(), null)
            Registry.register(EMOTE_DATA_SERIALIZER, ResourceLocation(MOD_ID, "unicode"), UnicodeEmoteData.Serializer)
        }

        @JvmStatic
		fun reloadConfig(): Boolean {
            val file = configDir!!.resolve("config.json").toFile()
            if (!file.exists()) {
                config = ChatEmotesConfig()
                return try {
                    if (!file.createNewFile()) {
                        LOGGER.error("Unable to create default config file")
                        return false
                    }
                    config!!.save(FileOutputStream(file))
                    true
                } catch (ex: IOException) {
                    LOGGER.error("Unable to write default config", ex)
                    false
                }
            }
            return try {
                config = ChatEmotesConfig.load(FileInputStream(file))
                LOGGER.info("Loaded config")
                true
            } catch (ex: IOException) {
                config = ChatEmotesConfig()
                LOGGER.error("Unable to read config", ex)
                false
            } catch (ex: JsonParseException) {
                config = ChatEmotesConfig()
                LOGGER.error("Unable to read config", ex)
                false
            }
        }
    }
}