package io.github.colintimbarndt.chat_emotes_util

import io.github.colintimbarndt.chat_emotes_util.model.PackLang
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

object PackLangProvider {
    @OptIn(ExperimentalSerializationApi::class)
    fun load(): Map<String, PackLang> {
        val languages = Json.decodeFromStream<List<String>>(streamAsset("/assets/packLangs.json")!!)
        return languages.associateWith {
            val raw: LinkedHashMap<String, String> =
                Json.decodeFromStream(streamAsset("/assets/chat_emotes/lang/$it.json")!!)
            raw["chat.emote"] = "%s"
            raw
        }
    }
}