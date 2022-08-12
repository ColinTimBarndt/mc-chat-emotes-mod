package io.github.colintimbarndt.chat_emotes.data

import io.github.colintimbarndt.chat_emotes.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.common.data.EmoteData
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener

object EmoteDataLoader : EmoteDataLoaderBase(), SimpleResourceReloadListener<List<EmoteData>> {
    override val serverMod get() = ChatEmotesServerMod
    override fun getFabricId() = resourceLoaderIdentifier
}