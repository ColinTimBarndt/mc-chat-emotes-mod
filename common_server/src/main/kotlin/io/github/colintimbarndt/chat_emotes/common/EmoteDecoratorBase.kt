package io.github.colintimbarndt.chat_emotes.common

import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.common.data.ChatEmote
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.fallback
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.plusAssign
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.contents.LiteralContents
import net.minecraft.server.level.ServerPlayer
import java.lang.Integer.min
import java.util.concurrent.CompletableFuture
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class EmoteDecoratorBase : ChatDecorator {
    companion object {
        private val FONT_BASE_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.WHITE)
        private val HIGHLIGHT_STYLE: Style = Style.EMPTY.withColor(ChatFormatting.YELLOW)
    }

    abstract val emoteDataLoader: EmoteDataLoaderBase
    abstract val config: ChatEmotesConfig

    override fun decorate(serverPlayer: ServerPlayer?, message: Component): CompletableFuture<Component> {
        return CompletableFuture.completedFuture(replaceEmotes(message))
    }

    private fun emoteForAlias(alias: String): ChatEmote? {
        val dataList = emoteDataLoader.loadedEmoteData
        for (data in dataList) {
            val result = data.emotesByAliasWithInnerColons[alias]
            if (result != null) return result
        }
        return null
    }

    private val maxCombo = 3

    @OptIn(ExperimentalContracts::class)
    internal inline fun emotesForAliasCombo(
        text: String,
        ends: IntArrayList,
        _start: Int,
        callback: (String, ChatEmote?, Int, Int) -> Unit
    ) {
        contract {
            callsInPlace(callback)
        }
        // TODO: Use string slices for more performance
        var start = _start
        var endLeftBound = 0
        Start@while (endLeftBound < ends.size) {
            val upperBound = min(endLeftBound + maxCombo, ends.size - 1)
            for (endIdx in upperBound downTo endLeftBound) {
                val end = ends.getInt(endIdx)
                val alias = text.substring(start, end)
                val emote = emoteForAlias(alias)
                if (emote != null || endIdx == endLeftBound) {
                    callback(alias, emote, start, end)
                    start = end + 2
                    endLeftBound = endIdx + 1
                    continue@Start
                }
            }
        }
    }

    private fun emoteForEmoticon(emoticon: String): ChatEmote? {
        if (!config.emoticons) return null
        val dataList = emoteDataLoader.loadedEmoteData
        for (data in dataList) {
            val result = data.emotesByEmoticon[emoticon]
            if (result != null) return result
        }
        return null
    }

    private fun createEmoteComponent(emote: ChatEmote, fallback: String): Component {
        var emoteStyle = FONT_BASE_STYLE.withFont(emote.font)
        if (emote.aliasWithColons != null) {
            emoteStyle = emoteStyle.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    literal(emote.aliasWithColons).setStyle(HIGHLIGHT_STYLE)
                )
            )
        }
        return fallback(
            literal(fallback),
            literal(emote.char.toString())
                .setStyle(emoteStyle)
        )
    }

    private fun replaceEmotes(comp: Component, filter: (ChatEmote) -> Boolean = { true }): Component {
        val content = comp.contents
        var mut: MutableComponent? = null

        if (content is LiteralContents) {
            val text = content.text
            val aliasEnds = IntArrayList(4) // Example: :x::y: is [2, 5]
            var startClip = 0
            var aliasStart = -1
            var startEmoticon = 0
            var i = 0

            fun addEmotes() {
                emotesForAliasCombo(text, aliasEnds, aliasStart) { raw, emote, start, end ->
                    if ((emote != null) && filter(emote)) {
                        val mut0 = mut ?: Component.empty().also { mut = it }
                        if (startClip < start - 1)
                            mut0 += text.substring(startClip, start - 1)
                        mut0 += createEmoteComponent(emote, ":$raw:")
                        startClip = end + 1
                    }
                }
                aliasStart = -1
                aliasEnds.clear()
            }

            while (i < text.length) {
                when (text[i]) {
                    ':' -> {
                        if (aliasStart == -1 || aliasStart == i) {
                            aliasStart = i + 1
                        } else {
                            aliasEnds += i
                            if (i + 1 < text.length && text[i + 1] == ':') {
                                // Combo like :x::y: is possible for alias "x:y"
                                i += 2 // skip double colon
                                continue
                            }
                            addEmotes()
                        }
                    }

                    ' ' -> {
                        if (aliasStart > 0) {
                            addEmotes()
                        }
                        if (startEmoticon != i) {
                            val emoticon = text.substring(startEmoticon, i)
                            val emote = emoteForEmoticon(emoticon)
                            if ((emote != null) && filter(emote)) {
                                val mut0 = mut ?: Component.empty().also { mut = it }
                                mut0 += text.substring(startClip, startEmoticon)
                                mut0 += createEmoteComponent(emote, emoticon)
                                startClip = i
                            }
                        }
                        startEmoticon = i + 1
                    }
                }
                i++
            }
            if (aliasStart > 0) {
                addEmotes()
            }
            if (startEmoticon != text.length) {
                val emoticon = text.substring(startEmoticon)
                val emote = emoteForEmoticon(emoticon)
                if (emote != null) {
                    val mut0 = mut ?: Component.empty().also { mut = it }
                    mut0 += text.substring(startClip, startEmoticon)
                    mut0 += createEmoteComponent(emote, emoticon)
                    startClip = text.length
                }
            }
            val mut0 = mut ?: return comp
            if (startClip != text.length) {
                mut0 += text.substring(startClip)
            }
            mut0 += comp.siblings
            return mut0
        }
        return comp
    }
}