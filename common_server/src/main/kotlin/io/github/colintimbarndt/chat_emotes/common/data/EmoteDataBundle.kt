package io.github.colintimbarndt.chat_emotes.common.data

import java.util.*
import kotlin.collections.HashMap

/**
 * Contains the Chat Emotes from a file
 */
data class EmoteDataBundle(
    val resourceLocation: ResourceLocation,
    private val emotes: ArrayList<ChatEmote>,
) : List<ChatEmote> by emotes {
    private val _emotesByAliasWithInnerColons = HashMap<String, ChatEmote>()

    val emotesByAliasWithInnerColons: Map<String, ChatEmote> =
        Collections.unmodifiableMap(_emotesByAliasWithInnerColons)

    private val _emotesByEmoticon = HashMap<String, ChatEmote>()

    val emotesByEmoticon: Map<String, ChatEmote> =
        Collections.unmodifiableMap(_emotesByEmoticon)

    private val _emotesByEmoji = HashMap<String, ChatEmote>()

    val emotesByEmoji: Map<String, ChatEmote> =
        Collections.unmodifiableMap(_emotesByEmoji)

    init {
        for (emote in emotes) {
            for (alias in emote.aliasesWithInnerColons) {
                _emotesByAliasWithInnerColons[alias] = emote
            }

            for (emoticon in emote.emoticons) {
                _emotesByEmoticon[emoticon] = emote
            }

            emote.emoji?.let { _emotesByEmoji[it] = emote }
        }
    }
}