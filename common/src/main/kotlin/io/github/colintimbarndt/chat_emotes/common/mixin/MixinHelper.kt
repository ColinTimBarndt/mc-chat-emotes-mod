package io.github.colintimbarndt.chat_emotes.common.mixin

import com.google.gson.JsonParseException
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import net.minecraft.locale.Language
import java.io.FileNotFoundException
import java.io.IOException
import java.util.function.BiConsumer

/**
 * Loads the language file using the given classes [ClassLoader], ignoring all keys starting with "`%`".
 * Will only print an error if it fails
 */
fun attemptLoadModLanguage(modClass: Class<*>, path: String, keyAdder: BiConsumer<String, String>) = try {
    modClass.getResourceAsStream(path).use { stream ->
        if (stream == null) throw FileNotFoundException()
        Language.loadFromJson(stream) { k, v -> if (!k.startsWith('%')) keyAdder.accept(k, v) }
    }
} catch (ex: JsonParseException) {
    LOGGER.error("Couldn't parse strings from {}", path, ex)
} catch (ex: IOException) {
    LOGGER.error("Couldn't read strings from {}", path, ex)
}
