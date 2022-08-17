@file:JvmName("ImportKt")

package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.WebHelper
import io.github.colintimbarndt.chat_emotes_util.lazyStringAsset
import io.github.colintimbarndt.chat_emotes_util.serial.*
import io.github.colintimbarndt.chat_emotes_util.streamAsset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.OutputStream

val latestEmojiFile = WebHelper.GithubFile("iamcal", "emoji-data", "master", "emoji.json")
val latestJoypixelsAliasesFile = WebHelper.GithubFile("joypixels", "emoji-toolkit", "master", "emoji_strategy.json")

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

fun Sequence<FlatEmojiData>.expandToEmoteData(aliasMapper: EmojiAliasSource.AliasMapper): Sequence<ExpandedEmoteData> {
    val categories = mutableMapOf<String, CategoryData>()
    lateinit var cat: CategoryData
    return map { data ->
        if (data.isMainEmoji) cat = categories.getOrPut(data.category, ::CategoryData)
        data.variation.let { emoji ->
            val aliases = aliasMapper.aliasesFor(data)
            ExpandedEmoteData(
                data,
                ChatEmoteData(data.name, emoji.unified.toString(), aliases, cat.nextChar())
            )
        }
    }
}

fun writeFonts(
    writer: PackWriter,
    data: Sequence<ExpandedEmoteData>,
    textures: TextureLoader.LoadedTextures,
    options: FontAssetOptions,
    namespace: String,
    fontName: EmojiData.() -> String,
) {
    val fonts = hashMapOf<String, FontWriter>()
    lateinit var fontWriter: FontWriter

    for (expanded in data) {
        val emoji = expanded.emoji
        val emote = expanded.emote
        if (emoji.isMainEmoji) {
            fontWriter = fonts.getOrPut(emoji.category) {
                writer.addFont(namespace, emoji.main.fontName(), options)
            }
        }
        textures[emoji.variation]?.let {
            fontWriter.addGlyph(emote.char, it)
        }
    }

    fonts.values.forEach(FontWriter::close)
}