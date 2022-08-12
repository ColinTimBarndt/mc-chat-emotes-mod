package io.github.colintimbarndt.chat_emotes.common

import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.fallback
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.plusAssign
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.contents.LiteralContents
import net.minecraft.server.level.ServerPlayer
import java.util.concurrent.CompletableFuture

abstract class EmoteDecoratorBase : ChatDecorator {
    companion object {
        private val FONT_BASE_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.WHITE)
        private val HIGHLIGHT_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.YELLOW)
    }

    abstract val emoteDataLoader: EmoteDataLoaderBase

    override fun decorate(serverPlayer: ServerPlayer?, message: Component): CompletableFuture<Component> {
        return CompletableFuture.completedFuture(replaceEmotes(message))
    }

    private fun emoteForAlias(alias: String): io.github.colintimbarndt.chat_emotes.common.data.Emote? {
        val dataList = emoteDataLoader.loadedEmoteData
        for (data in dataList) {
            val result = data.emoteForAlias(alias)
            if (result != null) return result
        }
        return null
    }

    private fun emoteForEmoticon(emoticon: String): io.github.colintimbarndt.chat_emotes.common.data.Emote? {
        val dataList = emoteDataLoader.loadedEmoteData
        for (data in dataList) {
            val result = data.emoteForEmoticon(emoticon)
            if (result != null) return result
        }
        return null
    }

    private fun createEmoteComponent(emote: io.github.colintimbarndt.chat_emotes.common.data.Emote, fallback: String): Component {
        var emoteStyle = FONT_BASE_STYLE.withFont(emote.font)
        if (emote.aliases.isNotEmpty()) {
            emoteStyle = emoteStyle.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    literal(":" + emote.aliases[0] + ":")
                        .setStyle(HIGHLIGHT_STYLE)
                )
            )
        }
        return fallback(
            literal(fallback),
            literal(emote.character.toString())
                .setStyle(emoteStyle)
        )
    }

    private fun replaceEmotes(comp: Component, filter: (io.github.colintimbarndt.chat_emotes.common.data.Emote) -> Boolean = { true }): Component {
        val content = comp.contents
        var mut: MutableComponent? = null
        if (content is LiteralContents) {
            val text = content.text
            var startClip = 0
            var startAlias = -1
            var startEmoticon = 0
            for (i in text.indices) {
                when (text[i]) {
                    ':' -> {
                        if (startAlias == -1 || startAlias == i) {
                            startAlias = i + 1
                        } else {
                            val alias = text.substring(startAlias until i)
                            val emote = emoteForAlias(alias)
                            if ((emote == null) || !filter(emote)) {
                                startAlias = i + 1
                            } else {
                                if (mut == null) mut = Component.empty()
                                mut!!
                                mut += text.substring(startClip until startAlias - 1)
                                mut += createEmoteComponent(emote, ":$alias:")
                                startClip = i + 1
                                startAlias = -1
                            }
                        }
                    }

                    ' ' -> {
                        startAlias = -1
                        if (startEmoticon != i) {
                            val emoticon = text.substring(startEmoticon until i)
                            val emote = emoteForEmoticon(emoticon)
                            if ((emote != null) && filter(emote)) {
                                if (mut == null) mut = Component.empty()
                                mut!!
                                mut += text.substring(startClip until startEmoticon)
                                mut += createEmoteComponent(emote, emoticon)
                                startClip = i
                            }
                        }
                        startEmoticon = i + 1
                    }
                }
            }
            if (startEmoticon != text.length) {
                val emoticon = text.substring(startEmoticon)
                val emote = emoteForEmoticon(emoticon)
                if (emote != null) {
                    if (mut == null) mut = Component.empty()
                    mut!!
                    mut += text.substring(startClip until startEmoticon)
                    mut += createEmoteComponent(emote, emoticon)
                    startClip = text.length
                }
            }
            if (mut == null) return comp
            if (startClip != text.length) {
                mut += text.substring(startClip)
            }
            mut += comp.siblings
            return mut
        }
        return comp
    }
}