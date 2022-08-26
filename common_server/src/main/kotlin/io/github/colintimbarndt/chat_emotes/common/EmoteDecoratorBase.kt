package io.github.colintimbarndt.chat_emotes.common

import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.common.data.ChatEmote
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataBundle
import io.github.colintimbarndt.chat_emotes.common.data.EmoteDataLoaderBase
import io.github.colintimbarndt.chat_emotes.common.data.PrefixTreeNode
import io.github.colintimbarndt.chat_emotes.common.permissions.EMOTES_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.PermissionsAdapter
import io.github.colintimbarndt.chat_emotes.common.permissions.VanillaPermissionsAdapter.hasPermission
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.fallback
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.plusAssign
import io.github.colintimbarndt.chat_emotes.common.util.HashedStringBuilder
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import net.minecraft.network.chat.Component.literal
import net.minecraft.network.chat.contents.LiteralContents
import net.minecraft.server.level.ServerPlayer
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
    abstract val permissionsAdapter: PermissionsAdapter

    override fun decorate(serverPlayer: ServerPlayer?, message: Component): CompletableFuture<Component> {
        return CompletableFuture.completedFuture(try {
            replaceEmotes(message) { bundle, emote ->
                permissionsAdapter.let {
                    serverPlayer?.hasPermission(emote.run {
                        val namespace = bundle.resourceLocation.namespace
                        val pack = bundle.resourceLocation.path
                        if (aliases.isNotEmpty()) "$EMOTES_PERMISSION.$namespace.$pack.${aliases[0]}"
                        else "$EMOTES_PERMISSION.$namespace.$pack"
                    })
                } ?: true
            }
        } catch (ex: Throwable) {
            LOGGER.error("Error parsing message:", ex)
            message
        })
    }

    private inline fun filteredEmoteByMap(
        key: String,
        map: (EmoteDataBundle) -> Map<String, ChatEmote>,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean,
    ): ChatEmote? {
        val dataList = emoteDataLoader.loadedEmoteData
        for (data in dataList) {
            val result = map(data)[key]
            if (result != null) {
                return if (filter(data, result)) result
                else null
            }
        }
        return null
    }

    private inline fun emoteForAlias(
        alias: String,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean,
    ): ChatEmote? = filteredEmoteByMap(alias, EmoteDataBundle::emotesByAliasWithInnerColons, filter)

    private inline fun emoteForEmoticon(
        emoticon: String,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean
    ): ChatEmote? = filteredEmoteByMap(emoticon, EmoteDataBundle::emotesByEmoticon, filter)

    private inline fun emoteForEmoji(
        emoji: String,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean
    ) = filteredEmoteByMap(emoji, EmoteDataBundle::emotesByEmoji, filter)

    @OptIn(ExperimentalContracts::class)
    internal inline fun emotesForAliasCombo(
        text: String,
        ends: IntArrayList,
        _start: Int,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean,
        callback: (String, ChatEmote?, Int, Int) -> Unit
    ) {
        contract {
            callsInPlace(callback)
        }
        var start = _start
        var endLeftBound = 0
        Start@ while (endLeftBound < ends.size) {
            var endIdx = endLeftBound
            var validEndIdx = 0
            var end = ends.getInt(endIdx)
            var alias = text.substring(start, end)
            var type = emoteDataLoader.aliasTree[alias]

            if (type == PrefixTreeNode.Invalid) {
                // No valid emote found
                callback(alias, null, start, end)
                start = end + 2
                endLeftBound++
                continue@Start
            }

            val aliasBuilder = HashedStringBuilder(alias)
            var endPrev = end
            if (++endIdx < ends.size) do {
                val endNext = ends.getInt(endIdx)
                aliasBuilder.append(text, endPrev, endNext)
                type = emoteDataLoader.aliasTree[aliasBuilder]
                if (type == PrefixTreeNode.Valid) {
                    validEndIdx = endIdx
                    end = endNext
                    alias = aliasBuilder.toString()
                }
                endPrev = endNext
                endIdx++
            } while (type != PrefixTreeNode.Invalid && endIdx < ends.size)
            // Valid emote (sequence) found
            val emote = emoteForAlias(alias, filter)
            callback(alias, emote, start, end)
            start = end + 2
            endLeftBound = validEndIdx + 1
        }
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

    private fun replaceEmotes(comp: Component, filter: (EmoteDataBundle, ChatEmote) -> Boolean): Component {
        val content = comp.contents
        var mut: MutableComponent? = null

        val enableAliases = config.aliases
        val enableEmoticons = config.emoticons
        val enableEmojis = config.emojis

        if ((enableAliases || enableEmoticons || enableEmojis) && content is LiteralContents) {
            val text = content.text
            val aliasEnds = IntArrayList(4) // Example: :x::y: is [2, 5]
            var startClip = 0
            var aliasStart = -1
            var startEmoticon = 0
            var i = 0

            fun addEmotes() {
                emotesForAliasCombo(text, aliasEnds, aliasStart, filter) { raw, emote, start, end ->
                    if (emote != null) {
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

            fun insertEmote(emote: ChatEmote, emoticon: String, from: Int, to: Int) {
                val mut0 = mut ?: Component.empty().also { mut = it }
                mut0 += text.substring(startClip, from)
                mut0 += createEmoteComponent(emote, emoticon)
                startClip = to
            }

            while (i < text.length) {
                when (val char = text[i]) {
                    ':' -> if (enableAliases) {
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

                    ' ' -> if (enableEmoticons) {
                        if (aliasStart > 0) addEmotes()
                        if (startEmoticon != i) {
                            val emoticon = text.substring(startEmoticon, i)
                            val emote = emoteForEmoticon(emoticon, filter)
                            if ((emote != null)) {
                                insertEmote(emote, emoticon, startEmoticon, i)
                            }
                        }
                        startEmoticon = i + 1
                    }

                    else -> if (enableEmojis) {
                        var state = emoteDataLoader.emojiTree[char]
                        if (state != PrefixTreeNode.Invalid) {
                            var emoji = if (state == PrefixTreeNode.Valid) char.toString() else null
                            val start = i
                            var end = i + 1
                            if (end < text.length) {
                                val emojiBuilder = HashedStringBuilder(text, start, end)
                                var endNext = end
                                var endPrev = endNext++
                                do {
                                    emojiBuilder.append(text, endPrev, endNext)
                                    state = emoteDataLoader.emojiTree[emojiBuilder]
                                    if (state == PrefixTreeNode.Valid) {
                                        emoji = emojiBuilder.toString()
                                        end = endNext
                                    }
                                    endPrev++
                                    endNext++
                                } while (state != PrefixTreeNode.Invalid && endNext <= text.length)
                            }
                            if (emoji != null) {
                                val emote = emoteForEmoji(emoji, filter)
                                if (emote != null) {
                                    if (aliasStart > 0) addEmotes()
                                    insertEmote(emote, emoji, start, end)
                                    i += emoji.length
                                    continue
                                }
                            }
                        }
                    }
                }
                i++
            }
            if (aliasStart > 0) addEmotes()
            if (startEmoticon != text.length) {
                val emoticon = text.substring(startEmoticon)
                val emote = emoteForEmoticon(emoticon, filter)
                if (emote != null) {
                    insertEmote(emote, emoticon, startEmoticon, text.length)
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