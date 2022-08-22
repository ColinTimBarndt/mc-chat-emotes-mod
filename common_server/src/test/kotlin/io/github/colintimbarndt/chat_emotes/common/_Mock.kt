@file:JvmName("MockKt")

package io.github.colintimbarndt.chat_emotes.common

import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.common.data.ChatEmote
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataBundle
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase
import net.minecraft.resources.ResourceLocation
import java.nio.file.Path
import kotlin.io.path.Path

object MockEmoteDataLoader : EmoteDataLoaderBase() {
    override val serverMod get() = MockServerMod

    val FOO_EMOTE = ChatEmote(
        name = "foo emote",
        aliases = arrayListOf("foo"),
        char = 'F',
        font = ResourceLocation("mock:emote/foo")
    )
    val BAR_EMOTE = ChatEmote(
        name = "bar emote",
        aliases = arrayListOf("bar"),
        char = 'B',
        font = ResourceLocation("mock:emote/bar")
    )
    val BAZ_EMOTE = ChatEmote(
        name = "baz emote",
        aliases = arrayListOf("baz"),
        char = 'A',
        font = ResourceLocation("mock:emote/baz")
    )
    val BAR_BAZ_EMOTE = ChatEmote(
        name = "bar:baz emote",
        aliases = arrayListOf("bar:baz"),
        char = 'Z',
        font = ResourceLocation("mock:emote/bar_baz")
    )

    init {
        loadedEmoteData = listOf(
            EmoteDataBundle(ResourceLocation("mock:emotes"), arrayListOf(
                FOO_EMOTE,
                BAR_EMOTE,
                BAZ_EMOTE,
                BAR_BAZ_EMOTE,
            ))
        )
    }
}

object MockServerMod : ChatEmotesServerModBase() {
    override var config = ChatEmotesConfig()
    override val configPath: Path = Path("")
    override val emoteDataLoader = MockEmoteDataLoader
    override val registries = Registries()
    override val emoteDecorator = object : EmoteDecoratorBase() {
        override val config: ChatEmotesConfig
            get() = this@MockServerMod.config
        override val emoteDataLoader: EmoteDataLoaderBase
            get() = this@MockServerMod.emoteDataLoader
    }
}