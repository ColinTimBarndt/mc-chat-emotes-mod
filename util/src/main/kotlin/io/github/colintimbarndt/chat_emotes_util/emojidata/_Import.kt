@file:JvmName("ImportKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.model.*
import io.github.colintimbarndt.chat_emotes_util.serial.FontAssetOptions
import io.github.colintimbarndt.chat_emotes_util.serial.PackWriter
import io.github.colintimbarndt.chat_emotes_util.serial.addFont
import io.github.colintimbarndt.chat_emotes_util.serial.addFonts
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
private val jsonPretty = Json { ignoreUnknownKeys = true; prettyPrint = true; coerceInputValues = true }

@OptIn(ExperimentalSerializationApi::class)
fun writeChatEmoteData(data: Sequence<ChatEmoteData>, stream: OutputStream, pretty: Boolean) {
    (if (pretty) jsonPretty else json).encodeToStream(SerializableSequence(data), stream)
}

data class FlatEmojiData(
    val main: EmojiData,
    val variation: BaseEmojiData,
    val variationCombo: CharSequence? = null,
) {
    val category inline get() = main.category
    val name inline get() = main.name
    val isMainEmoji inline get() = variationCombo == null
}

fun Sequence<EmojiData>.flatEmojiData() = sequence {
    for (mainEmoji in this@flatEmojiData) {
        yield(FlatEmojiData(mainEmoji, mainEmoji))
        mainEmoji.skinVariations?.forEach { (variationCombo, variation) ->
            yield(FlatEmojiData(mainEmoji, variation, variationCombo))
        }
    }
}

data class ExpandedEmoteData(
    val emoji: FlatEmojiData,
    val emote: ChatEmoteData,
)

private inline fun Sequence<FlatEmojiData>.expand(
    mapper: EmojiSourceMapper,
    font: ResourceKey
): Sequence<ExpandedEmoteData> {
    var char = ' '
    return map { data ->
        if (char == '\u0000') throw IndexOutOfBoundsException("Out of characters")
        val emoji = data.variation
        ExpandedEmoteData(
            data,
            ChatEmoteData(
                name = mapper.nameFor(data),
                category = mapper.categoryFor(data),
                aliases = mapper.aliasesFor(data),
                emoticons = mapper.emoticonsFor(data),
                emoji = emoji.unified.toString(),
                char = char++,
                font = font
            )
        )
    }
}

fun <G : Any> Sequence<FlatEmojiData>.expandToEmoteData(
    mapper: EmojiSourceMapper,
    fontNamespace: String,
    fontName: FontNaming<G>,
): Sequence<Pair<ResourceKey, Sequence<ExpandedEmoteData>>> {
    return if (fontName.groupKeyType == Unit::class) {
        // No grouping needed, can stream
        @Suppress("UNCHECKED_CAST")
        val key = Unit as G
        val font = ResourceKey(fontNamespace, fontName.getFontName(key))
        sequenceOf(font to expand(mapper, font))
    } else {
        groupBy(fontName::getGroupKey).asSequence().map { (key, list) ->
            val font = ResourceKey(fontNamespace, fontName.getFontName(key))
            font to list.asSequence().expand(mapper, font)
        }
    }
}

fun writeFonts(
    writer: PackWriter,
    groupedData: Sequence<Pair<ResourceKey, Sequence<ExpandedEmoteData>>>,
    textures: TextureLoader.LoadedTextures,
    options: FontAssetOptions,
    sharedNamespace: String,
    sharedName: String,
) {
    writer.addFonts(sharedNamespace, sharedName, options) {
        for ((font, members) in groupedData) {
            addFont(font.namespace, font.path) {
                for (member in members) {
                    val emoji = member.emoji
                    val emote = member.emote
                    textures[emoji.variation]?.let {
                        addGlyph(emote.char, it)
                    }
                }
            }
        }
    }
}