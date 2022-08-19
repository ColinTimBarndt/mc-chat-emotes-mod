@file:JvmName("ImportKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.lazyStringAsset
import io.github.colintimbarndt.chat_emotes_util.model.*
import io.github.colintimbarndt.chat_emotes_util.serial.*
import io.github.colintimbarndt.chat_emotes_util.streamAsset
import io.github.colintimbarndt.chat_emotes_util.web.GithubFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.OutputStream

val latestEmojiFile = GithubFile("iamcal", "emoji-data", "master", "emoji.json")
val latestJoypixelsAliasesFile = GithubFile("joypixels", "emoji-toolkit", "master", "emoji_strategy.json")

val includedEmojiCommitHash by lazyStringAsset("/assets/emoji.json.hash")

private val json = Json { ignoreUnknownKeys = true }
private val jsonPretty = Json { ignoreUnknownKeys = true; prettyPrint = true }

@OptIn(ExperimentalSerializationApi::class)
fun streamEmojiData(jsonStream: InputStream): Sequence<EmojiData> =
    json.decodeToSequence(jsonStream, DecodeSequenceMode.ARRAY_WRAPPED)

fun streamIncludedEmojiData() = streamEmojiData(streamAsset("/assets/emoji.json")!!)

@OptIn(ExperimentalSerializationApi::class)
fun loadJoypixelsEmojiData(jsonStream: InputStream): JoypixelsEmojiData =
    json.decodeFromStream(jsonStream)

fun loadIncludedJoypixelsAliases() = loadJoypixelsEmojiData(streamAsset("/assets/joypixelsAliases.json")!!)

private data class CategoryData(private var char: Char = '\u0020') {
    fun nextChar(): Char {
        if (char == '\uffee') throw Error("out of characters")
        else return char++
    }
}

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
    aliasMapper: EmojiAliasSource.AliasMapper,
    font: ResourceKey
): Sequence<ExpandedEmoteData> {
    var char = ' '
    return map { data ->
        if (char == '\u0000') throw IndexOutOfBoundsException("Out of characters")
        val emoji = data.variation
        val aliases = aliasMapper.aliasesFor(data)
        ExpandedEmoteData(
            data,
            ChatEmoteData(
                name = data.name,
                emoji = emoji.unified.toString(),
                aliases = aliases,
                char = char++,
                font = font
            )
        )
    }
}

@Suppress("UNCHECKED_CAST")
fun <G : Any> Sequence<FlatEmojiData>.expandToEmoteData(
    aliasMapper: EmojiAliasSource.AliasMapper,
    fontNamespace: String,
    fontName: FontNaming<G>
): Sequence<Pair<ResourceKey, Sequence<ExpandedEmoteData>>> {
    return if (fontName.groupKeyType == Unit::class) {
        // No grouping needed, can stream
        val key = Unit as G
        val font = ResourceKey(fontNamespace, fontName.getFontName(key))
        sequenceOf(font to expand(aliasMapper, font))
    } else {
        groupBy(fontName::getGroupKey).asSequence().map { (key, list) ->
            val font = ResourceKey(fontNamespace, fontName.getFontName(key))
            font to list.asSequence().expand(aliasMapper, font)
        }
    }
}

fun writeFonts(
    writer: ZipPackWriter,
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