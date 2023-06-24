package io.github.colintimbarndt.chat_emotes.common

import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentFactory
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractImmutableComponentBuilder
import io.github.colintimbarndt.chat_emotes.common.abstraction.ChatColor
import io.github.colintimbarndt.chat_emotes.common.config.ChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.common.data.*
import io.github.colintimbarndt.chat_emotes.common.permissions.EMOTES_PERMISSION
import io.github.colintimbarndt.chat_emotes.common.permissions.PermissionsAdapter
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.EMOTE_TRANSLATION_KEY
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.fallback
import io.github.colintimbarndt.chat_emotes.common.util.HashedStringBuilder
import it.unimi.dsi.fastutil.ints.IntArrayList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class EmoteDecoratorBase<P, Component>(
    private val componentFactory: AbstractComponentFactory<Component>,
) {
    companion object {
        private val FONT_BASE_COLOR: ChatColor = ChatColor.WHITE
        private val HIGHLIGHT_COLOR: ChatColor = ChatColor.YELLOW
    }

    protected abstract val permissionsAdapter: PermissionsAdapter<P, *>

    abstract val emoteData: EmoteDataSource
    abstract val config: ChatEmotesConfig

    fun decorateSync(player: P?, message: Component): Component {
        return try {
            replaceEmotes(message) { bundle, emote ->
                if (player != null) {
                    permissionsAdapter.playerHasPermission(player, emote.run {
                        val namespace = bundle.resourceLocation.namespace
                        val pack = bundle.resourceLocation.path
                        if (aliases.isNotEmpty()) "$EMOTES_PERMISSION.$namespace.$pack.${aliases[0]}"
                        else "$EMOTES_PERMISSION.$namespace.$pack"
                    })
                } else true
            }
        } catch (ex: Throwable) {
            LOGGER.error("Error parsing message:", ex)
            message
        }
    }

    private inline fun filteredEmoteByMap(
        key: String,
        map: (EmoteDataBundle) -> Map<String, ChatEmote>,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean,
    ): ChatEmote? {
        val dataList = emoteData.loadedEmoteData
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
        cStart: Int,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean,
        callback: (String, ChatEmote?, Int, Int) -> Unit
    ) {
        contract {
            callsInPlace(callback)
        }
        var start = cStart
        var endLeftBound = 0
        Start@ while (endLeftBound < ends.size) {
            var endIdx = endLeftBound
            var end = ends.getInt(endIdx)
            var alias = text.substring(start, end)
            var type = emoteData.aliasTree[alias]

            // Quickly discard without creating a string builder
            if (type == PrefixTreeNode.Invalid) {
                // No valid emote found
                callback(alias, null, start, end)
                start = end + 2 // skip "::"
                endLeftBound++
                continue@Start
            }

            // Search for combination (e.g. :foo::bar:)
            val aliasBuilder = HashedStringBuilder(alias)
            var endPrev = end
            var validEndIdx = endIdx
            if (++endIdx < ends.size) do {
                val endNext = ends.getInt(endIdx)
                aliasBuilder.append(text, endPrev, endNext)
                type = emoteData.aliasTree[aliasBuilder]
                if (type == PrefixTreeNode.Valid) {
                    validEndIdx = endIdx
                    end = endNext
                    alias = aliasBuilder.toString()
                }
                endPrev = endNext
                endIdx++
            } while (type != PrefixTreeNode.Invalid && endIdx < ends.size)
            // Valid emote (sequence) found or invalid
            val emote = emoteForAlias(alias, filter)
            callback(alias, emote, start, end)
            start = end + 2 // skip "::"
            endLeftBound = validEndIdx + 1
        }
    }

    private fun createEmoteComponent(emote: ChatEmote, fallback: String): Component =
        componentFactory.run {
            var component = fallback(
                text(fallback),
                literal(emote.char.toString())
                    .font(emote.font)
                    .color(FONT_BASE_COLOR)
                    .build(),
                EMOTE_TRANSLATION_KEY
            )
            val colons = emote.aliasWithColons
            if (colons != null) {
                component = component.onHover {
                    showText(
                        literal(colons)
                            .color(HIGHLIGHT_COLOR)
                            .build()
                    )
                }
            }
            return component.build()
        }

    private fun replaceEmotes(
        comp: Component,
        filter: (EmoteDataBundle, ChatEmote) -> Boolean
    ): Component = componentFactory.run {
        val content = comp.literalContent()
        var mut: AbstractImmutableComponentBuilder<Component>? = null

        val enableAliases = config.aliases
        val enableEmoticons = config.emoticons
        val enableEmojis = config.emojis

        if ((enableAliases || enableEmoticons || enableEmojis) && content.isPresent) {
            val text = content.get()
            val aliasEnds = IntArrayList(4) // Example: :x::y: is [2, 5]
            var startClip = 0
            var aliasStart = -1
            var startEmoticon = 0
            var i = 0

            fun addEmotes() {
                emotesForAliasCombo(text, aliasEnds, aliasStart, filter) { raw, emote, start, end ->
                    if (emote != null) {
                        var mut0 = mut ?: empty()
                        if (startClip < start - 1)
                            mut0 += text.substring(startClip, start - 1)
                        mut = mut0 + createEmoteComponent(emote, ":$raw:")
                        startClip = end + 1
                    }
                }
                aliasStart = -1
                aliasEnds.clear()
            }

            fun insertEmote(emote: ChatEmote, emoticon: String, from: Int, to: Int) {
                mut = (mut ?: empty()) +
                        text.substring(startClip, from) +
                        createEmoteComponent(emote, emoticon)
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
                        var state = emoteData.emojiTree[char]
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
                                    state = emoteData.emojiTree[emojiBuilder]
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
            var mut0 = mut ?: return comp
            if (startClip != text.length) {
                mut0 += text.substring(startClip)
            }
            return mut0
                .append(comp.siblingComponents)
                .build()
        }
        return comp
    }
}