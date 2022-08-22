package io.github.colintimbarndt.chat_emotes_util

import io.github.colintimbarndt.chat_emotes_util.model.PackLang
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

object PackLangProvider {
    @OptIn(ExperimentalSerializationApi::class)
    fun load(): Map<String, PackLang> = Json.decodeFromStream(streamAsset("/assets/packLang.json")!!)
}