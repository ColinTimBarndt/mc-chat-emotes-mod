package io.github.colintimbarndt.chat_emotes.forge.config

import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.*

internal class ForgeChatEmotesConfig(builder: Builder) {
    private val aliases = builder.define("aliases", true)
    private val emoticons = builder.define("emoticons", true)
    private val emojis = builder.define("emojis", true)
    private val maxCombinedEmote =
        builder.defineInRange("maxCombinedEmote", 4, 1, Int.MAX_VALUE, Int::class.java)

    val unmodifiable
        get() = ChatEmotesConfig(
            aliases = aliases.get(),
            emoticons = emoticons.get(),
            emojis = emojis.get(),
            maxCombinedEmote = maxCombinedEmote.get(),
        )

    companion object {
        val SPEC: ForgeConfigSpec
        val INSTANCE: ForgeChatEmotesConfig
        init {
            val pair = Builder().configure { ForgeChatEmotesConfig(it) }
            SPEC = pair.right
            INSTANCE = pair.left
        }
    }
}