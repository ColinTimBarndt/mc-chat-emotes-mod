@file:JvmName("EmziDiscordKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.model

import kotlinx.serialization.Serializable

typealias DiscordEmojiMap = Map<UnicodeSequence, DiscordEmojiList.Definition>

@JvmInline
@Serializable
value class DiscordEmojiList(
    val emojiDefinitions: ArrayList<Definition>,
) {
    @Serializable
    data class Definition(
        val primaryName: String,
        val names: ArrayList<String>,
        val surrogates: String,
        val surrogatesAlternate: String? = null,
        val category: String,
    )
}

inline fun DiscordEmojiList.toMap(): DiscordEmojiMap =
    emojiDefinitions.flatMap { def ->
        sequence {
            yield(UnicodeSequence(def.surrogates) to def)
            def.surrogatesAlternate?.let {
                yield(UnicodeSequence(it) to def)
            }
        }
    }.toMap()