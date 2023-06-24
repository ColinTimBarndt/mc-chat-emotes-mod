package io.github.colintimbarndt.chat_emotes.data

import io.github.colintimbarndt.chat_emotes.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataBundle
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase
import io.github.colintimbarndt.chat_emotes.common.util.toMinecraft
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener

object EmoteDataLoader : EmoteDataLoaderBase(), SimpleResourceReloadListener<List<EmoteDataBundle>> {
    override val config get() = ChatEmotesServerMod.config
    override fun getFabricId() = resourceLoaderIdentifier.toMinecraft()
}
