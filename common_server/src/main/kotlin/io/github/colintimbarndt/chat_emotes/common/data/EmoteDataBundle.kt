package io.github.colintimbarndt.chat_emotes.common.data

import net.minecraft.resources.ResourceLocation
import java.lang.Integer.max
import java.util.Collections

/**
 * Contains the Chat Emotes from a file
 */
data class EmoteDataBundle(
    val resourceLocation: ResourceLocation,
    private val emotes: ArrayList<ChatEmote>,
) : List<ChatEmote> by emotes {
    @Transient
    private val _emotesByAliasWithInnerColons: HashMap<String, ChatEmote> = HashMap()

    @Transient
    val emotesByAliasWithInnerColons: Map<String, ChatEmote> =
        Collections.unmodifiableMap(_emotesByAliasWithInnerColons)

    @Transient
    private val _emotesByEmoticon: HashMap<String, ChatEmote> = HashMap()

    @Transient
    val emotesByEmoticon: Map<String, ChatEmote> =
        Collections.unmodifiableMap(_emotesByEmoticon)

    @Transient
    val maxCombinedEmote: Int

    init {
        var max = 0
        for (emote in emotes) {
            for (alias in emote.aliasesWithInnerColons) {
                _emotesByAliasWithInnerColons[alias] = emote
                max = max(max, alias.count { it == ':' })
            }

            for (emoticon in emote.emoticons) {
                _emotesByEmoticon[emoticon] = emote
            }
        }
        // Divide by two, as colons are `::`
        maxCombinedEmote = max shr 1
    }
}