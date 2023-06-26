package io.github.colintimbarndt.chat_emotes.fabric.data

import io.github.colintimbarndt.chat_emotes.fabric.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.common.mod.data.EmoteDataLoaderBase
import io.github.colintimbarndt.chat_emotes.common.mod.util.toMinecraft
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener

object EmoteDataLoader : EmoteDataLoaderBase(), IdentifiableResourceReloadListener {
    override val config get() = ChatEmotesServerMod.config
    override fun getFabricId() = resourceLoaderIdentifier.toMinecraft()
}
