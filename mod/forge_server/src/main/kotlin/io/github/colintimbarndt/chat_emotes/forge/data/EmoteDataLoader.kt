package io.github.colintimbarndt.chat_emotes.forge.data

import io.github.colintimbarndt.chat_emotes.forge.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.common.mod.data.EmoteDataLoaderBase

object EmoteDataLoader : EmoteDataLoaderBase() {
    override val config get() = ChatEmotesServerMod.config
}