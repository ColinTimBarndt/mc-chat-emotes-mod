package io.github.colintimbarndt.chat_emotes.fabric

import io.github.colintimbarndt.chat_emotes.common.mod.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.fabric.config.ConfigWatcher
import io.github.colintimbarndt.chat_emotes.fabric.data.EmoteDataLoader
import io.github.colintimbarndt.chat_emotes.common.mod.permissions.VanillaPermissionsAdapter
import kotlinx.serialization.SerializationException
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.packs.PackType
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path

object ChatEmotesServerMod :
    ChatEmotesServerModBase(),
    DedicatedServerModInitializer {
    internal lateinit var configPath: Path
    private var configWatcher: ConfigWatcher? = null
    override val emoteDataLoader = EmoteDataLoader

    override fun onInitializeServer() {
        val loader = FabricLoader.getInstance()
        val confDir = loader.configDir.resolve(MOD_ID)
        val configDirFile = confDir.toFile()
        configPath = confDir
        if (!(configDirFile.isDirectory || configDirFile.mkdirs())) {
            LOGGER.error("Failed to create config directory")
        } else {
            reloadConfig()
            ServerLifecycleEvents.SERVER_STARTED.register {
                synchronized(this) {
                    configWatcher?.interrupt()
                    configWatcher = ConfigWatcher().apply { start() }
                }
            }
            ServerLifecycleEvents.SERVER_STOPPING.register {
                synchronized(this) {
                    configWatcher?.interrupt()
                    configWatcher = null
                }
            }
        }
        ServerMessageDecoratorEvent.EVENT.register(
            ServerMessageDecoratorEvent.STYLING_PHASE,
            emoteDecorator
        )
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(emoteDataLoader)

        permissionsAdapter = VanillaPermissionsAdapter
    }

    /**
     * Reloads the config and updates it in a thread-safe (atomic) manner
     */
    fun reloadConfig(): Boolean {
        val file = configPath.resolve("config.json").toFile()
        var myConfig = ChatEmotesConfig.DEFAULT
        if (!file.exists()) {
            return try {
                if (!file.createNewFile()) {
                    LOGGER.error("Unable to create default config file")
                    return false
                }
                myConfig.save(FileOutputStream(file))
                true
            } catch (ex: IOException) {
                LOGGER.error("Unable to write default config", ex)
                false
            } finally {
                config = myConfig
            }
        }
        return try {
            myConfig = ChatEmotesConfig.load(FileInputStream(file))
            true
        } catch (ex: IOException) {
            myConfig = ChatEmotesConfig.DEFAULT
            LOGGER.error("Unable to read config", ex)
            false
        } catch (ex: SerializationException) {
            myConfig = ChatEmotesConfig.DEFAULT
            LOGGER.error("Unable to parse config", ex)
            false
        } finally {
            config = myConfig
        }
    }
}