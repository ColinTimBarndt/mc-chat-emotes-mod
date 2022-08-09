package io.github.colintimbarndt.chat_emotes.data.unicode

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.github.colintimbarndt.chat_emotes.data.Emote
import io.github.colintimbarndt.chat_emotes.data.EmoteDataSerializer
import io.github.colintimbarndt.chat_emotes.data.unicode.UnicodeModifierType.Companion.build
import io.github.colintimbarndt.chat_emotes.data.unicode.joiner.UnicodeJoiner
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern
import io.github.colintimbarndt.chat_emotes.util.StringBuilderExt.plusAssign
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraft.util.GsonHelper.getAsJsonObject
import net.minecraft.util.GsonHelper.isStringValue
import java.text.ParseException
import java.util.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Suppress("NOTHING_TO_INLINE")
object UnicodeEmoteDataSerializer : EmoteDataSerializer<UnicodeEmoteData> {
    private val FONTS: Int2ObjectMap<ResourceLocation> = Int2ObjectOpenHashMap(8)
    private val CODE_POINT_PATTERN = Pattern.compile(
        "(?<min>[\\da-f]{1,6})(-(?<max>[\\da-f]{1,6}))?", Pattern.CASE_INSENSITIVE
    )
    private val SINGLE_CODE_POINT_PATTERN = Pattern.compile(
        "^[\\da-f]{1,6}$", Pattern.CASE_INSENSITIVE
    )
    private val EMPTY_STRING_ARRAY = arrayOf<String>()

    private class State {
        /**
         * Maps an alias to a unicode sequence
         */
        val allAliases = HashMap<String, String>()

        /**
         * Maps a unicode sequence to aliases
         */
        val emoteAliases = TreeMap<String, Array<String>>()

        /**
         * Maps an unicode sequence to [Emoticons](https://en.wikipedia.org/wiki/Emoticon)
         */
        val emoticons = HashMap<String, MutableList<String>>()
    }

    @Throws(JsonSyntaxException::class)
    override fun read(
        location: ResourceLocation, json: JsonObject, samples: Set<String>?
    ): UnicodeEmoteData {
        val state = State()

        getAsJsonObject(json, "symbols").let { symbolsJson ->
            val deferredSymbols = parseSymbols(symbolsJson, state, samples)
            resolveDeferredSymbols(deferredSymbols, state, symbolsJson, samples)
        }

        if (json.has("emoticons")) {
            val emoticonsJson = getAsJsonObject(json, "emoticons")
            parseEmoticons(emoticonsJson, state)
        }

        val instance = UnicodeEmoteData(location)

        // Create emotes
        val usedCodePoints = IntRBTreeSet()
        for ((seq, aliases) in state.emoteAliases) {
            val emoticons = state.emoticons[seq]
            val emote = createEmote(
                location, usedCodePoints, seq, aliases, emoticons?.toTypedArray() ?: EMPTY_STRING_ARRAY
            )
            instance.emotesMut.add(emote)
            instance.emotesByUnicodeSequence[seq] = emote
            for (alias in aliases) instance.emotesByAlias[alias] = emote
            for (emoticon in emote.emoticons) instance.emotesByEmoticon[emoticon] = emote
        }

        // Modifiers
        if (json.has("modifiers")) {
            if (samples == null) {
                throw JsonSyntaxException("Modifiers require a list of samples")
            }
            val modifiersJson = getAsJsonObject(json, "modifiers")
            val modifiers = parseModifiers(modifiersJson, state)
            applyModifiers(modifiers, location, usedCodePoints, instance, samples)
        }

        return instance
    }

    private inline fun parseModifiers(modifiersJson: JsonObject, state: State): ArrayList<UnicodeModifierType> {
        val modifiers: ArrayList<UnicodeModifierType> = ArrayList<UnicodeModifierType>(modifiersJson.size())

        // Parse
        for ((name, modifierJsonEl) in modifiersJson.entrySet()) {
            if (!modifierJsonEl.isJsonObject) {
                throw JsonSyntaxException(
                    "Expected modifiers['$name'] to be an object, got " + GsonHelper.getType(modifierJsonEl)
                )
            }
            val modifierJson = modifierJsonEl.asJsonObject
            if (!GsonHelper.isObjectNode(modifierJson, "values")) {
                throw JsonSyntaxException(
                    "Expected modifiers['$name'].values to be an object, got " + GsonHelper.getType(modifierJson["values"])
                )
            }
            val valuesJson = modifierJson.getAsJsonObject("values")
            if (!isStringValue(modifierJson, "format")) {
                throw JsonSyntaxException(
                    "Expected modifiers['$name'].format to be a string, got " + GsonHelper.getType(modifierJson["format"])
                )
            }
            val formatString = GsonHelper.getAsString(modifierJson, "format")
            var defaultValue = 0
            if (GsonHelper.isNumberValue(modifierJson, "default")) {
                defaultValue = GsonHelper.getAsInt(modifierJson, "default")
            } else if (modifierJson.has("replace")) {
                throw JsonSyntaxException(
                    "Expected modifiers['$name'].option to be a number, got ${GsonHelper.getType(modifierJson["option"])}(required because 'replace' is specified)"
                )
            }
            val builder = build(name, defaultValue, formatString)
            if (modifierJson.has("priority")) {
                val priorityJson = modifierJson["priority"]
                if (!GsonHelper.isNumberValue(priorityJson)) {
                    throw JsonSyntaxException(
                        "Expected modifiers['$name'].priority to be a number, got " + GsonHelper.getType(priorityJson)
                    )
                }
                builder.priority(priorityJson.asInt)
            }
            val replaceLen =
                if (modifierJson.has("replace")) {
                    val strings = parseModifiersReplace(modifierJson, name)
                    for ((i, patternStr) in strings.withIndex()) {
                        try {
                            builder.replace(Pattern.compile(patternStr))
                        } catch (ex: PatternSyntaxException) {
                            throw JsonSyntaxException(
                                "Value modifiers['$name'].replace[$i] ('$patternStr') is not a valid regular expression",
                                ex
                            )
                        }
                    }
                    strings.size
                } else -1
            val zwj = getBooleanOptional(modifierJson["zwj"]) { "modifiers['$name'].zwj" }
            val vs16 = getBooleanOptional(modifierJson["vs16"]) { "modifiers['$name'].vs16" }
            for ((key, valueJson) in valuesJson.entrySet()) {
                val seq = StringBuilder()
                if (zwj) seq += ZWJ
                if (SINGLE_CODE_POINT_PATTERN.matcher(key).matches()) {
                    // Literal code point
                    seq.appendCodePoint(key.toInt(16))
                } else {
                    // Reference to alias
                    val ref: String = state.allAliases[key] ?: throw JsonSyntaxException(
                        "Key '$key' in modifiers['$name'].values is not an emote alias"
                    )
                    seq += ref
                }
                if (vs16) seq += VS16
                val names = parseModifierValues(valueJson, name, key)
                for (i in names.indices) {
                    val mName = names[i]
                    if (builder.isNameUsed(mName)) {
                        throw JsonSyntaxException(
                            "Value modifiers['$name'].values['$key'][$i] ('${names[i]}') is not unique in this modifier"
                        )
                    }
                }
                if (replaceLen >= 0 && names.size != replaceLen) {
                    throw JsonSyntaxException(
                        "Length of modifiers['$name'].values['$key'] does not match the length of modifiers['$name'].replace"
                    )
                }
                builder.addVariant(seq.toString(), names)
                if (defaultValue >= names.size) {
                    throw JsonSyntaxException(
                        "Value modifiers['$name'].values['$key'].names does not have enough elements"
                    )
                }
            }
            val mod = builder.create()
            modifiers.add(mod)
        }

        return modifiers
    }

    private inline fun applyModifiers(
        modifiers: List<UnicodeModifierType>,
        location: ResourceLocation,
        used: IntRBTreeSet,
        instance: UnicodeEmoteData,
        samples: Set<String>
    ) {
        val modifierValues = TreeMap<String, UnicodeModifierType.Modifier>()
        for (modifier in modifiers) {
            for (modValue in modifier.values) {
                modifierValues[modValue.sequence] = modValue
            }
        }

        // Apply
        for (sample in samples) {
            var i = Int.MAX_VALUE
            var modLen = 0
            var mod: UnicodeModifierType.Modifier? = null
            for ((name, value) in modifierValues) {
                val idx = sample.indexOf(name, 1)
                if (idx != -1 && idx < i) {
                    mod = value
                    modLen = name.length
                    i = idx
                }
            }
            if (i == Int.MAX_VALUE) continue
            val mods = ArrayList<UnicodeModifierType.Modifier>(1)
            val sb = StringBuilder(sample.length - modLen)
            var clip = 0
            do {
                mods.add(mod!!)
                sb.append(sample, clip, i)
                clip = i + modLen
                if (clip == sample.length) break
                i = Int.MAX_VALUE
                for ((name, value) in modifierValues) {
                    val idx = sample.indexOf(name, clip)
                    if (idx != -1 && idx < i) {
                        mod = value
                        modLen = name.length
                        i = idx
                    }
                }
            } while (i != Int.MAX_VALUE)
            sb.append(sample, clip, sample.length)
            mods.sort() // Sort grouped by type
            // Add modified emote
            val baseEmote: Emote? = run {
                var baseSeq = sb.toString()
                if (baseSeq.codePointCount(0, baseSeq.length) == 2 && baseSeq[baseSeq.length - 1] == VS16) {
                    // Text mode emoji
                    baseSeq = baseSeq.substring(0, baseSeq.length - 1)
                }
                instance.emoteForUnicodeSequence(baseSeq)
            }
            if (baseEmote != null) {
                val aliasArray = ArrayList<String>(baseEmote.aliases.size)
                // transform aliases
                for (alias in baseEmote.aliases) {
                    var alias0 = alias
                    for (m in mods) {
                        alias0 = m.getModifiedName(alias)
                    }
                    aliasArray += alias0
                }
                val newAliases = aliasArray.toTypedArray()
                val e = createEmote(location, used, sample, newAliases)
                instance.emotesMut += e
                for (alias in aliasArray) instance.emotesByAlias[alias] = e
                instance.emotesByUnicodeSequence[sample] = e
            }
        }
    }

    /**
     * Parses the "emoticons" section of the JSON object
     */
    private inline fun parseEmoticons(
        emoticonsJson: JsonObject, state: State
    ) {
        for ((key, value) in emoticonsJson.entrySet()) {
            val emos = parseEmoticons(value, key).toMutableList()
            val seq = state.allAliases[key] ?: throw JsonSyntaxException(
                "Key '$key' in emoticons is not an emote alias"
            )
            // TODO
            val existing = if (state.emoticons.containsKey(seq)) {
                state.emoticons[seq]!!
            } else {
                val a = ArrayList<String>()
                state.emoticons[seq] = a
                a
            }
            existing.addAll(emos)
            state.emoticons[seq] = emos
        }
    }

    /**
     * Parses the "symbols" section of the JSON object
     */
    private inline fun parseSymbols(
        symbolsJson: JsonObject, state: State, samples: Set<String>?
    ): HashMap<String, UnicodePattern> {
        val deferredSymbols = HashMap<String, UnicodePattern>()
        for ((symbolKey, symbolJson) in symbolsJson.entrySet()) {
            val m = CODE_POINT_PATTERN.matcher(symbolKey)
            if (m.matches()) {
                // is a codepoint or range
                val min = m.group("min").toInt(16)
                val maxStr = m.group("max")
                if (maxStr == null) {
                    // codepoint
                    val aliases = parseAliases(symbolJson, symbolKey)
                    val seq = Character.toString(min)
                    for (alias in aliases) {
                        state.allAliases[alias] = seq
                    }
                    state.emoteAliases[seq] = aliases
                    continue
                }
                // range
                if (!symbolJson.isJsonArray) {
                    throw JsonSyntaxException(
                        "Expected symbols['$symbolKey'] to be a JsonArray, was " + GsonHelper.getType(symbolJson)
                    )
                }
                val max = maxStr.toInt(16)
                val subArray = symbolJson.asJsonArray
                var c = min
                for (i in 0 until subArray.size()) {
                    val subValue = subArray[i]
                    if (GsonHelper.isNumberValue(subValue)) {
                        val inc = subValue.asInt
                        if (inc < 1) {
                            throw JsonSyntaxException(
                                "Symbols skipped must be greater than 0 in symbols['$symbolKey']"
                            )
                        }
                        c += inc
                        continue
                    }
                    val aliases = parseAliases(subValue, symbolKey, i)
                    val seq = Character.toString(c)
                    for (alias in aliases) {
                        state.allAliases[alias] = seq
                    }
                    state.emoteAliases[seq] = aliases
                    c++
                }
                if (c - 1 > max) {
                    throw JsonSyntaxException(
                        "Symbols out of range in symbols['$symbolKey']"
                    )
                }
                continue
            } // end codepoint or range
            // pattern
            val pattern: UnicodePattern = try {
                UnicodePattern.parse(symbolKey)
            } catch (ex: ParseException) {
                throw JsonSyntaxException(
                    "Expected key '$symbolKey' in symbols to be a code point (-range) or pattern", ex
                )
            }
            val hasAllRefs = pattern.getNameReferences().all(state.allAliases::containsKey)
            if (hasAllRefs) {
                pattern.resolveNames { state.allAliases[it]!! }
                resolvePatternEntry(symbolKey, pattern, symbolJson, samples, state)
            } else {
                deferredSymbols[symbolKey] = pattern
            }
        }
        return deferredSymbols
    }

    /**
     * Resolves all symbols that could not be resolved by [parseSymbols].
     * This can happen when a referenced alias is defined after the reference
     */
    private inline fun resolveDeferredSymbols(
        deferredSymbols: HashMap<String, UnicodePattern>, state: State, symbols: JsonObject, samples: Set<String>?
    ) {
        // Resolve patterns
        val removableKeys = HashSet<String>()
        while (deferredSymbols.size > 0) {
            removableKeys.clear()
            for ((key1, pattern) in deferredSymbols) {
                val hasAllRefs = pattern.getNameReferences().all(state.allAliases::containsKey)
                if (hasAllRefs) {
                    removableKeys.add(key1)
                    pattern.resolveNames { state.allAliases[it]!! }
                    resolvePatternEntry(key1, pattern, symbols[key1], samples, state)
                }
            }
            if (removableKeys.size == 0) {
                throw JsonSyntaxException(
                    "Undefined or circular references in symbol patterns: " + java.lang.String.join(
                        ", ", deferredSymbols.keys
                    )
                )
            }
            for (rem in removableKeys) {
                deferredSymbols.remove(rem)
            }
        }
    }

    @Throws(JsonSyntaxException::class)
    private fun resolvePatternEntry(
        key: String, pattern: UnicodePattern, json: JsonElement, samples: Iterable<String>?, state: State
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
                    if (isStringValue(entry)) {
                        val j = UnicodeJoiner.parse(entry.asString)
                        joiners[i] = j
                        if (!j.isCompatibleWith(pattern)) {
                            throw JsonSyntaxException(
                                "Joiner '${json.asString}' is incompatible with pattern '$key'"
                            )
                        }
                    } else {
                        throw JsonSyntaxException(
                            "Expected symbols['$key'][$i] to be a string, was " + GsonHelper.getType(entry)
                        )
                    }
                }
            } else if (isStringValue(json)) {
                joiners = arrayOfNulls(1)
                val j = UnicodeJoiner.parse(json.asString)
                joiners[0] = j
                if (!j.isCompatibleWith(pattern)) {
                    throw JsonSyntaxException(
                        "Joiner '${json.asString}' is incompatible with pattern '$key'"
                    )
                }
            } else {
                throw JsonSyntaxException(
                    "Expected symbols['$key'] to be a string or JsonArray, was " + GsonHelper.getType(json)
                )
            }
        } catch (ex: ParseException) {
            throw JsonSyntaxException(
                "Invalid unicode joiner in symbols['$key']", ex
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
                    state.allAliases[alias!!] = sample
                }
                state.emoteAliases[sample] = aliases.requireNoNulls()
            }
        }
    }

    /**
     * Creates an emote object instance which uses a unique combination of [Char] and font
     */
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

    /**
     * Gets the resource location of a sub-font using an index [i], will cache entries
     */
    private fun getFont(base: ResourceLocation, i: Int): ResourceLocation {
        val existing = FONTS[i]
        if (existing != null) return existing
        val rl = ResourceLocation(base.namespace, base.path + i)
        FONTS.put(i, rl)
        return rl
    }

    /**
     * @see [parseNames]
     */
    private fun parseModifierValues(
        valueJson: JsonElement, name: String, key: String
    ) = parseNames(valueJson) { "modifiers['$name'].values['$key']" }

    /**
     * @see [parseNames]
     */
    @Throws(JsonSyntaxException::class)
    private fun parseModifiersReplace(modifierJson: JsonObject, name: String) =
        parseNames(modifierJson["replace"]) { "modifiers['$name'].replace" }

    /**
     * @see [parseNames]
     */
    @Throws(JsonSyntaxException::class)
    private fun parseAliases(
        json: JsonElement, key: String
    ) = parseNames(json) { "symbols['$key']" }

    /**
     * @see [parseNames]
     */
    @Throws(JsonSyntaxException::class)
    private fun parseAliases(
        json: JsonElement, key: String, subKey: Int
    ) = parseNames(json) { "symbols['$key'][$subKey]" }

    /**
     * @see [parseNames]
     */
    @Throws(JsonSyntaxException::class)
    private fun parseEmoticons(
        json: JsonElement, key: String
    ) = parseNames(json) { "emoticons[$key]" }

    /**
     * Parses a JSON element which can either be a [String] or an [Array] of Strings
     */
    @Throws(JsonSyntaxException::class)
    private inline fun parseNames(
        json: JsonElement, path: () -> String
    ): Array<String> {
        if (isStringValue(json)) {
            return arrayOf(json.asString)
        }
        if (json.isJsonArray) {
            val array = json.asJsonArray
            val aliases = arrayOfNulls<String>(array.size())
            for ((i, alias) in array.withIndex()) {
                if (!isStringValue(alias)) {
                    throw JsonSyntaxException(
                        "Expected ${path()}[$i] to be a string, was " + GsonHelper.getType(alias)
                    )
                }
                aliases[i] = alias.asString
            }
            return aliases.requireNoNulls()
        }
        throw JsonSyntaxException(
            "Expected " + path() + " to be a string or JsonArray, was " + GsonHelper.getType(json)
        )
    }

    private inline fun getBooleanOptional(
        json: JsonElement?, path: () -> String
    ): Boolean {
        if (json == null) return false
        return if (GsonHelper.isBooleanValue(json)) {
            json.asJsonPrimitive.asBoolean
        } else {
            throw JsonSyntaxException(
                "Expected ${path()} to be a boolean, got " + GsonHelper.getType(json)
            )
        }
    }
}