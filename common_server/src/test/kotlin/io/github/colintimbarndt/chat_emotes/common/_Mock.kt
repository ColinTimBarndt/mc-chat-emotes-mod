@file:JvmName("MockKt")

package io.github.colintimbarndt.chat_emotes.common

import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentBuilder
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentFactory
import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.common.data.*
import io.github.colintimbarndt.chat_emotes.common.permissions.NoPermissionsAdapter
import java.util.*

val MOCK_CONFIG = ChatEmotesConfig()

object MockEmoteDataLoader : EmoteDataSource {
    override lateinit var loadedEmoteData: List<EmoteDataBundle>
        private set
    override val aliasTree = AliasPrefixTree(HashMap())
    override val emojiTree = EmojiPrefixTree()

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
    val BAR_FOO_BAZ_EMOTE = ChatEmote(
        name = "bar:foo:baz emote",
        aliases = arrayListOf("bar:foo:baz"),
        char = 'X',
        font = ResourceLocation("mock:emote/bar_foo_baz")
    )

    init {
        loadedEmoteData = listOf(
            EmoteDataBundle(
                ResourceLocation("mock:emotes"), arrayListOf(
                    FOO_EMOTE,
                    BAR_EMOTE,
                    BAZ_EMOTE,
                    BAR_BAZ_EMOTE,
                    BAR_FOO_BAZ_EMOTE
                )
            )
        )
        aliasTree.load(loadedEmoteData, 3)
    }
}

object MockEmoteDecorator : EmoteDecoratorBase<Any?, Unit>(MockComponentFactory) {
    override val config = MOCK_CONFIG
    override val permissionsAdapter = NoPermissionsAdapter
    override val emoteData = MockEmoteDataLoader
}

object MockComponentFactory : AbstractComponentFactory<Unit>() {
    override fun literal(text: String): AbstractComponentBuilder<Unit> =
        throw RuntimeException("Mock")

    override val Unit.siblingComponents: List<Unit>
        get() = throw RuntimeException("Mock")

    override fun Unit.literalContent(): Optional<String> =
        throw RuntimeException("Mock")

    override fun <T> Unit.visit(visitor: (text: String) -> Optional<T>): Optional<T> =
        throw RuntimeException("Mock")

    override fun translatable(key: String, with: List<Unit>, fallback: String?): AbstractComponentBuilder<Unit> =
        throw RuntimeException("Mock")
}