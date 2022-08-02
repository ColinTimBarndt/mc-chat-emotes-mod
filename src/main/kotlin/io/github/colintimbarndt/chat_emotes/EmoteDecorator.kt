package io.github.colintimbarndt.chat_emotes

import io.github.colintimbarndt.chat_emotes.data.Emote
import io.github.colintimbarndt.chat_emotes.util.ComponentUtils.fallback
import io.github.colintimbarndt.chat_emotes.util.ComponentUtils.withStyle
import io.github.colintimbarndt.chat_emotes.util.ComponentUtils.plusAssign
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ChatDecorator
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.LiteralContents
import net.minecraft.server.level.ServerPlayer
import java.util.concurrent.CompletableFuture

object EmoteDecorator {
    val EMOTES = ChatDecorator { sender, message -> CompletableFuture.completedFuture(replaceEmotes(sender, message)) }

    private val FONT_BASE_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.WHITE)
    private val HIGHLIGHT_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.YELLOW)

    private fun emoteForAlias(alias: String) : Emote? {
        val dataList = ChatEmotesMod.EMOTE_DATA_LOADER.loadedEmoteData
        for (data in dataList) {
            val result = data.emoteForAlias(alias)
            if (result != null) return result
        }
        return null
    }

    private fun emoteForEmoticon(emoticon: String): Emote? {
        val dataList = ChatEmotesMod.EMOTE_DATA_LOADER.loadedEmoteData
        for (data in dataList) {
            val result = data.emoteForEmoticon(emoticon)
            if (result != null) return result
        }
        return null
    }

    private fun createEmoteComponent(emote: Emote, fallback: String): Component {
        return fallback(
            literal(fallback),
            literal(emote.character.toString())
                .withStyle(FONT_BASE_STYLE) {
                    withFont(emote.font)
                    withHoverEvent(
                        HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            literal(":" + emote.aliases[0] + ":")
                                .setStyle(HIGHLIGHT_STYLE)
                        )
                    )
                }
        )
    }

    private fun replaceEmotes(sender: ServerPlayer?, comp: Component): Component {
        val content = comp.contents
        var mut : MutableComponent? = null
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
                            val alias = text.substring(startAlias..i)
                            val emote = emoteForAlias(alias)
                            if (emote == null) {
                                startAlias = i + 1
                            } else {
                                if (mut == null) mut = Component.empty()
                                mut!!
                                mut += text.substring(startClip, startAlias - 1)
                                mut += createEmoteComponent(emote, ":$alias:")
                                startClip = i + 1
                                startAlias = -1
                            }
                        }
                    }
                    ' ' -> {
                        startAlias = -1
                        if (startEmoticon != i) {
                            val emoticon = text.substring(startEmoticon..i)
                            val emote = emoteForEmoticon(emoticon)
                            if (emote != null) {
                                if (mut == null) mut = Component.empty()
                                mut!!
                                mut += text.substring(startClip, startEmoticon)
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
                    mut += text.substring(startClip, startEmoticon)
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