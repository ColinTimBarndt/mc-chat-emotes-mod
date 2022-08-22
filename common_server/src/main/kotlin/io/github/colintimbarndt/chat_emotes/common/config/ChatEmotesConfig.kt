package io.github.colintimbarndt.chat_emotes.common.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@Serializable
class ChatEmotesConfig {
    // TODO: Add configuration options

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(IOException::class)
    fun save(output: OutputStream) {
        json.encodeToStream(this, output)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        @OptIn(ExperimentalSerializationApi::class)
        @Throws(IOException::class, kotlinx.serialization.SerializationException::class)
        fun load(stream: InputStream?) =
            stream?.let(json::decodeFromStream) ?: ChatEmotesConfig()
    }
}