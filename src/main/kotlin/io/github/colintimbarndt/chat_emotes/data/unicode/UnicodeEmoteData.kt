package io.github.colintimbarndt.chat_emotes.data.unicode

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.github.colintimbarndt.chat_emotes.data.Emote
import io.github.colintimbarndt.chat_emotes.data.EmoteData
import io.github.colintimbarndt.chat_emotes.data.EmoteDataSerializer
import io.github.colintimbarndt.chat_emotes.data.FontGenerator
import io.github.colintimbarndt.chat_emotes.data.unicode.joiner.UnicodeJoiner
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import java.io.IOException
import java.nio.file.Path
import java.text.ParseException
import java.util.*
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.imageio.ImageIO

class UnicodeEmoteData private constructor(override val location: ResourceLocation) : EmoteData {
    private val emotesMut: MutableSet<Emote> = HashSet()
    private val emotesByUnicodeSequence: MutableMap<String, Emote> = TreeMap()
    private val emotesByAlias: MutableMap<String, Emote> = HashMap()
    private val emotesByEmoticon: MutableMap<String, Emote> = HashMap()

    override val emotes: Set<Emote> get() = Collections.unmodifiableSet(emotesMut)

    override val aliases: Set<String> get() = Collections.unmodifiableSet(emotesByAlias.keys)

    override val emoticons: Set<String> get() = emotesByEmoticon.keys

    override fun emoteForUnicodeSequence(sequence: String): Emote? {
        return emotesByUnicodeSequence[sequence]
    }

    override fun emoteForAlias(alias: String): Emote? {
        return emotesByAlias[alias]
    }

    override fun emoteForEmoticon(emoticon: String): Emote? {
        return emotesByEmoticon[emoticon]
    }

    override val serializer = Serializer

    @Throws(IOException::class)
    override fun generateFonts(gen: FontGenerator, imageSources: Path) {
        // TODO: optimize
        val fonts = HashMap<ResourceLocation, FontGenerator.Font>(8)
        try {
            EmoteTextureArchive(imageSources.toFile()).use { images ->
                for (emote in emotesMut) {
                    val seq = emote.unicodeSequence ?: continue
                    val texture = images.getTextureAsStream(seq)
                    if (texture != null) {
                        val img = ImageIO.read(texture)
                        val fontId = emote.font
                        val font: FontGenerator.Font?
                        if (fonts.containsKey(fontId)) {
                            font = fonts[fontId]
                        } else {
                            font = gen.createFont(fontId, img.width)
                            fonts[fontId] = font
                        }
                        font!!.addSprite(img, emote.character)
                    }
                }
            }
        } finally {
            for (font in fonts.values) {
                font.close()
            }
        }
    }

    object Serializer : EmoteDataSerializer<UnicodeEmoteData> {
        @Throws(JsonSyntaxException::class)
        override fun read(
            location: ResourceLocation,
            json: JsonObject,
            samples: Set<String>?
        ): UnicodeEmoteData {
            val instance = UnicodeEmoteData(location)
            // Maps an alias to a unicode sequence
            val allAliases = HashMap<String?, String>()
            // Maps a unicode sequence to aliases
            val emoteAliases = TreeMap<String, Array<String>>()
            // Maps an unicode sequence to emoticons
            val emoticons = HashMap<String, MutableList<String>>()
            val deferredSymbols = HashMap<String, UnicodePattern>()
            val symbols = GsonHelper.getAsJsonObject(json, "symbols")
            for ((key1, value) in symbols.entrySet()) {
                val m = CODE_POINT_PATTERN.matcher(key1)
                if (m.matches()) {
                    // is a codepoint or range
                    val min = m.group("min").toInt(16)
                    val maxStr = m.group("max")
                    if (maxStr == null) {
                        // codepoint
                        val aliases = parseAliases(value, key1)
                        val seq = Character.toString(min)
                        for (alias in aliases) {
                            allAliases[alias] = seq
                        }
                        emoteAliases[seq] = aliases
                        continue
                    }
                    // range
                    if (!value.isJsonArray) {
                        throw JsonSyntaxException(
                            "Expected symbols['$key1'] to be a JsonArray, was "
                                    + GsonHelper.getType(value)
                        )
                    }
                    val max = maxStr.toInt(16)
                    val subArray = value.asJsonArray
                    var c = min
                    for (i in 0 until subArray.size()) {
                        val subValue = subArray[i]
                        if (GsonHelper.isNumberValue(subValue)) {
                            val inc = subValue.asInt
                            if (inc < 1) {
                                throw JsonSyntaxException(
                                    "Symbols skipped must be greater than 0 in symbols['$key1']"
                                )
                            }
                            c += inc
                            continue
                        }
                        val aliases = parseAliases(subValue, key1, i)
                        val seq = Character.toString(c)
                        for (alias in aliases) {
                            allAliases[alias] = seq
                        }
                        emoteAliases[seq] = aliases
                        c++
                    }
                    if (c - 1 > max) {
                        throw JsonSyntaxException(
                            "Symbols out of range in symbols['$key1']"
                        )
                    }
                    continue
                } // end codepoint or range
                // pattern
                val pattern: UnicodePattern = try {
                    UnicodePattern.parse(key1)
                } catch (ex: ParseException) {
                    throw JsonSyntaxException(
                        "Expected key '$key1' in symbols to be a code point (-range) or pattern",
                        ex
                    )
                }
                val hasAllRefs = pattern.getNameReferences().all(allAliases::containsKey)
                if (hasAllRefs) {
                    pattern.resolveNames { allAliases[it]!! }
                    resolvePatternEntry(key1, pattern, value, samples, allAliases, emoteAliases)
                } else {
                    deferredSymbols[key1] = pattern
                }
            }
            run {
                // Resolve patterns
                val removableKeys = HashSet<String>()
                while (deferredSymbols.size > 0) {
                    removableKeys.clear()
                    for ((key1, pattern) in deferredSymbols) {
                        val hasAllRefs = pattern.getNameReferences().all(allAliases::containsKey)
                        if (hasAllRefs) {
                            removableKeys.add(key1)
                            pattern.resolveNames { allAliases[it]!! }
                            resolvePatternEntry(key1, pattern, symbols[key1], samples, allAliases, emoteAliases)
                        }
                    }
                    if (removableKeys.size == 0) {
                        throw JsonSyntaxException(
                            "Undefined or circular references in symbol patterns: " +
                                    java.lang.String.join(", ", deferredSymbols.keys)
                        )
                    }
                    for (rem in removableKeys) {
                        deferredSymbols.remove(rem)
                    }
                }
            }
            // Emoticons
            if (json.has("emoticons")) {
                val emoticonsJson = GsonHelper.getAsJsonObject(json, "emoticons")
                for ((key, value) in emoticonsJson.entrySet()) {
                    val emos = parseEmoticons(value, key).toMutableList()
                    val seq = allAliases[key]
                        ?: throw JsonSyntaxException(
                            "Key '$key' in emoticons is not an emote alias"
                        )
                    val existing = emoticons[seq]!!
                    existing.addAll(emos)
                    emoticons[seq] = emos
                }
            }
            val used = IntRBTreeSet()
            for ((seq, aliases) in emoteAliases) {
                val emot = emoticons[seq]!!
                val e = createEmote(
                    location, used, seq, aliases, emot.toTypedArray()
                )
                instance.emotesMut.add(e)
                instance.emotesByUnicodeSequence[seq] = e
                for (alias in aliases) instance.emotesByAlias[alias] = e
                for (emoticon in e.emoticons) instance.emotesByEmoticon[emoticon] = e
            }
            return instance
        }

        @Throws(JsonSyntaxException::class)
        private fun resolvePatternEntry(
            key: String,
            pattern: UnicodePattern,
            json: JsonElement,
            samples: Iterable<String>?,
            allAliases: MutableMap<String?, String>,
            emoteAliases: MutableMap<String, Array<String>>
        ) {
            // TODO: Optimize for single match
            if (samples == null) {
                throw JsonSyntaxException(
                    "Patterns require a list of samples"
                )
            }
            val joiners: Array<UnicodeJoiner?>
            try {
                if (json.isJsonArray) {
                    val array = json.asJsonArray
                    joiners = arrayOfNulls(array.size())
                    for (i in joiners.indices) {
                        val entry = array[i]
                        if (GsonHelper.isStringValue(entry)) {
                            val j = UnicodeJoiner.parse(entry.asString)
                            joiners[i] = j
                            if (!j.isCompatibleWith(pattern)) {
                                throw JsonSyntaxException(
                                    "Joiner '" + json.asString + "' is incompatible with pattern '" + key + "'"
                                )
                            }
                        } else {
                            throw JsonSyntaxException(
                                "Expected symbols['" + key + "'][" + i + "] to be a string, was " +
                                        GsonHelper.getType(entry)
                            )
                        }
                    }
                } else if (GsonHelper.isStringValue(json)) {
                    joiners = arrayOfNulls(1)
                    val j = UnicodeJoiner.parse(json.asString)
                    joiners[0] = j
                    if (!j.isCompatibleWith(pattern)) {
                        throw JsonSyntaxException(
                            "Joiner '" + json.asString + "' is incompatible with pattern '" + key + "'"
                        )
                    }
                } else {
                    throw JsonSyntaxException(
                        "Expected symbols['" + key + "'] to be a string or JsonArray, was " +
                                GsonHelper.getType(json)
                    )
                }
            } catch (ex: ParseException) {
                throw JsonSyntaxException(
                    "Invalid unicode joiner in symbols['$key']",
                    ex
                )
            }
            // Find matches
            for (sample in samples) {
                val result = pattern.invoke(sample)
                if (result != null) {
                    val aliases = arrayOfNulls<String>(joiners.size)
                    for (i in joiners.indices) {
                        aliases[i] = joiners[i]!!.evaluate(result)
                    }
                    for (alias in aliases) {
                        allAliases[alias] = sample
                    }
                    emoteAliases[sample] = aliases.requireNoNulls()
                }
            }
        }

        private fun createEmote(
            base: ResourceLocation,
            used: IntRBTreeSet,
            seq: String,
            aliases: Array<String>,
            emoticons: Array<String> = EMPTY_STRING_ARRAY
        ): Emote {
            require(seq.isNotEmpty()) { "Empty emote unicode sequence" }
            var cp = seq.codePointAt(0)
            if (used.contains(cp)) {
                cp += 100 shl 16
                val usedIt = used.tailSet(cp).iterator()
                while (usedIt.hasNext() && usedIt.nextInt() == cp) cp++
                if (used.contains(cp)) throw AssertionError()
            }
            used.add(cp)
            val font = getFont(base, cp shr 16)
            val ch = (cp and 0xffff).toChar()
            return Emote(font, ch, aliases, emoticons, seq)
        }

        private fun getFont(base: ResourceLocation, i: Int): ResourceLocation {
            val existing = FONTS[i]
            if (existing != null) return existing
            val rl = ResourceLocation(base.namespace, base.path + i)
            FONTS.put(i, rl)
            return rl
        }

        private val CODE_POINT_PATTERN = Pattern.compile(
            "(?<min>[\\da-f]{1,6})(-(?<max>[\\da-f]{1,6}))?",
            Pattern.CASE_INSENSITIVE
        )
        private val EMPTY_STRING_ARRAY = arrayOf<String>()
        @Throws(JsonSyntaxException::class)
        private fun parseAliases(
            json: JsonElement,
            key: String
        ): Array<String> {
            return parseNames(json) { "symbols['$key']" }
        }

        @Throws(JsonSyntaxException::class)
        private fun parseAliases(
            json: JsonElement,
            key: String,
            subKey: Int
        ): Array<String> {
            return parseNames(json) { "symbols['$key'][$subKey]" }
        }

        @Throws(JsonSyntaxException::class)
        private fun parseEmoticons(
            json: JsonElement,
            key: String
        ): Array<String> {
            return parseNames(json) { "emoticons[$key]" }
        }

        @Throws(JsonSyntaxException::class)
        private fun parseNames(
            json: JsonElement,
            path: Supplier<String>
        ): Array<String> {
            if (GsonHelper.isStringValue(json)) {
                return arrayOf(json.asString)
            }
            if (json.isJsonArray) {
                val array = json.asJsonArray
                val aliases = arrayOfNulls<String>(array.size())
                for (i in aliases.indices) {
                    val alias = array[i]
                    if (!GsonHelper.isStringValue(alias)) {
                        throw JsonSyntaxException(
                            "Expected " + path.get() +
                                    "[" + i + "] to be a string, was " +
                                    GsonHelper.getType(alias)
                        )
                    }
                    aliases[i] = alias.asString
                }
                return aliases.requireNoNulls()
            }
            throw JsonSyntaxException(
                "Expected " + path.get() +
                        " to be a string or JsonArray, was " +
                        GsonHelper.getType(json)
            )
        }

        private val FONTS: Int2ObjectMap<ResourceLocation> = Int2ObjectOpenHashMap(8)
    }
}