package io.github.colintimbarndt.chat_emotes

import io.github.colintimbarndt.chat_emotes.common.EmoteDecoratorBase
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase

object EmoteDecorator : EmoteDecoratorBase() {
    override val emoteDataLoader: EmoteDataLoaderBase
        get() = ChatEmotesServerMod.emoteDataLoader
}