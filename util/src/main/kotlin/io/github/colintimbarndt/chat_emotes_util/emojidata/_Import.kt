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

private data class CategoryData(var char: Char = '\u0020') {
    fun next() {
        if (char == '\uffee') throw Error("out of characters")
        else char++
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun writeChatEmoteData(data: Sequence<ChatEmoteData>, stream: OutputStream, pretty: Boolean) {
    (if (pretty) jsonPretty else json).encodeToStream(SerializableSequence(data), stream)
}

fun convertToChatEmoteData(data: Sequence<EmojiData>) = sequence {
    val categories = mutableMapOf<String, CategoryData>()
    for (ed in data) ed.run outer@ {
        val cat = categories.getOrPut(category, ::CategoryData)
        val aliases = shortNames.map { it.replace('-', '_') }
        yield(Triple(this, this, ChatEmoteData(name, unified.toString(), aliases, cat.char)))
        cat.next()
        if (ed.skinVariations != null) {
            for ((combo, edv) in ed.skinVariations) edv.run {
                // Skin tone = 2 chars, `_toneX` = 6 chars
                val suffix0 = StringBuilder(3 * combo.length)
                combo.codePoints().forEach {
                    // See https://www.unicode.org/reports/tr51/#Diversity
                    if (it !in 0x1F3FB..0x1F3FF) {
                        throw RuntimeException("invalid skin tone ${it.toChar()} (0x${it.toString(16)}) in variation $combo ($name)")
                    }
                    val tone = (it - 0x1F3FB + '1'.code).toChar()
                    suffix0.append("_tone").append(tone)
                }
                val suffix = suffix0.toString()
                val aliasesV = aliases.map { it + suffix }
                yield(Triple(this@outer, this, ChatEmoteData(name, unified.toString(), aliasesV, cat.char)))
                cat.next()
            }
        }
    }
}

suspend fun writeFonts(
    writer: PackWriter,
    data: Sequence<Triple<EmojiData, SimpleEmojiData, ChatEmoteData>>,
    textureSource: EmojiTextureSource,
    options: FontAssetOptions,
    size: Int,
    clean: Boolean,
    namespace: String,
    fontName: EmojiData.() -> String,
) {
    val textures = textureSource.loader.load(size, clean)
    val fonts = hashMapOf<String, FontWriter>()

    for ((base, emoji, emote) in data) {
        val fontWriter = fonts.getOrPut(base.category) {
            writer.addFont(namespace, base.fontName(), options)
        }
        textures[emoji]?.also {
            fontWriter.addGlyph(emote.char, it)
        }
    }

    fonts.values.forEach(FontWriter::close)
}