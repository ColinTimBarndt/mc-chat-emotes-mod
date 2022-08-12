package io.github.colintimbarndt.chat_emotes.common

import com.google.gson.JsonParseException
import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path

abstract class ChatEmotesServerModBase {
    abstract var config: ChatEmotesConfig protected set
    abstract val configPath: Path
    abstract val emoteDataLoader: EmoteDataLoaderBase
    abstract val registries: Registries
    abstract val emoteDecorator: EmoteDecoratorBase

    fun reloadConfig(): Boolean {
        val file = configPath.resolve("config.json").toFile()
        if (!file.exists()) {
            config = ChatEmotesConfig()
            return try {
                if (!file.createNewFile()) {
                    LOGGER.error("Unable to create default config file")
                    return false
                }
                config.save(FileOutputStream(file))
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
            LOGGER.error("Unable to parse config", ex)
            false
        }
    }
}