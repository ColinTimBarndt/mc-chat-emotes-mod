package io.github.colintimbarndt.chat_emotes.data.unicode;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.colintimbarndt.chat_emotes.data.Emote;
import io.github.colintimbarndt.chat_emotes.data.EmoteDataSerializer;
import io.github.colintimbarndt.chat_emotes.data.unicode.joiner.UnicodeJoiner;
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.github.colintimbarndt.chat_emotes.data.unicode.UnicodeUtil.VS16;
import static io.github.colintimbarndt.chat_emotes.data.unicode.UnicodeUtil.ZWJ;
import static net.minecraft.util.GsonHelper.*;

public final class UnicodeEmoteDataSerializer implements EmoteDataSerializer<UnicodeEmoteData> {
    private static final Pattern CODE_POINT_PATTERN = Pattern.compile(
            "^(?<min>[\\da-f]{1,6})(-(?<max>[\\da-f]{1,6}))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SINGLE_CODE_POINT_PATTERN = Pattern.compile(
            "^[\\da-f]{1,6}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final String[] EMPTY_STRING_ARRAY = {};

    UnicodeEmoteDataSerializer() {
    }

    @Override
    public @NotNull UnicodeEmoteData read(
            @NotNull ResourceLocation location,
            final @NotNull JsonObject json,
            final @Nullable Set<String> samples
    ) throws JsonSyntaxException {
        final var instance = new UnicodeEmoteData(location);
        // Maps an alias to a unicode sequence
        final var allAliases = new HashMap<String, String>();
        // Maps a unicode sequence to aliases
        final var emoteAliases = new TreeMap<String, String[]>();
        // Maps an unicode sequence to emoticons
        final var emoticons = new HashMap<String, List<String>>();

        final var deferredSymbols = new HashMap<String, UnicodePattern>();
        final var symbols = getAsJsonObject(json, "symbols");

        for (var entry : symbols.entrySet()) {
            final var key = entry.getKey();
            final var value = entry.getValue();
            final var m = CODE_POINT_PATTERN.matcher(key);
            if (m.matches()) {
                // is a codepoint or range
                final int min = Integer.parseInt(m.group("min"), 16);
                final var maxStr = m.group("max");
                if (maxStr == null) {
                    // codepoint
                    final var aliases = parseAliases(value, key);
                    addCodePoint(samples, allAliases, emoteAliases, min, aliases);
                    continue;
                }
                // range
                if (!value.isJsonArray()) {
                    throw new JsonSyntaxException(
                            "Expected symbols['" + key + "'] to be an array, was "
                                    + getType(value)
                    );
                }
                final int max = Integer.parseInt(maxStr, 16);
                final var subArray = value.getAsJsonArray();
                int c = min;
                for (int i = 0; i < subArray.size(); i++) {
                    final var subValue = subArray.get(i);
                    if (isNumberValue(subValue)) {
                        final int inc = subValue.getAsInt();
                        if (inc < 1) {
                            throw new JsonSyntaxException(
                                    "Symbols skipped must be greater than 0 in symbols['" + key + "']"
                            );
                        }
                        c += inc;
                        continue;
                    }
                    final var aliases = parseAliases(subValue, key, i);
                    addCodePoint(samples, allAliases, emoteAliases, c, aliases);
                    c++;
                }
                if (c - 1 > max) {
                    throw new JsonSyntaxException(
                            "Symbols out of range in symbols['" + key + "']"
                    );
                }
                continue;
            } // end codepoint or range
            // pattern
            final UnicodePattern pattern;
            try {
                pattern = UnicodePattern.parse(key);
            } catch (ParseException ex) {
                throw new JsonSyntaxException(
                        "Expected key '" + key + "' in symbols to be a code point (-range) or pattern",
                        ex
                );
            }
            final var wrapper = new Object() {
                boolean hasAllRefs = true;
            };
            pattern.getNameReferences(ref -> wrapper.hasAllRefs &= allAliases.containsKey(ref));
            if (wrapper.hasAllRefs) {
                pattern.resolveNames(allAliases::get);
                resolvePatternEntry(key, pattern, value, samples, allAliases, emoteAliases);
            } else {
                deferredSymbols.put(key, pattern);
            }
        }
        { // Resolve patterns
            final var removableKeys = new HashSet<String>();
            while (deferredSymbols.size() > 0) {
                removableKeys.clear();
                for (Map.Entry<String, UnicodePattern> entry : deferredSymbols.entrySet()) {
                    final var pattern = entry.getValue();
                    final var wrapper = new Object() {
                        boolean hasAllRefs = true;
                    };
                    pattern.getNameReferences(ref -> wrapper.hasAllRefs &= allAliases.containsKey(ref));
                    if (wrapper.hasAllRefs) {
                        final String key = entry.getKey();
                        removableKeys.add(key);
                        pattern.resolveNames(allAliases::get);
                        resolvePatternEntry(key, pattern, symbols.get(key), samples, allAliases, emoteAliases);
                    }
                }
                if (removableKeys.size() == 0) {
                    throw new JsonSyntaxException(
                            "Undefined or circular references in symbol patterns: " +
                                    String.join(", ", deferredSymbols.keySet())
                    );
                }
                for (String rem : removableKeys) {
                    deferredSymbols.remove(rem);
                }
            }
        }
        // Emoticons
        if (json.has("emoticons")) {
            final var emoticonsJson = getAsJsonObject(json, "emoticons");
            for (Map.Entry<String, JsonElement> entry : emoticonsJson.entrySet()) {
                final var key = entry.getKey();
                final var emos = parseEmoticons(entry.getValue(), key);
                final var seq = allAliases.get(key);
                if (seq == null) {
                    throw new JsonSyntaxException(
                            "Key '" + key + "' in emoticons is not an emote alias"
                    );
                }
                final var existing = emoticons.get(seq);
                if (existing != null) {
                    existing.addAll(List.of(emos));
                } else {
                    emoticons.put(seq, Arrays.asList(emos));
                }
            }
        }
        // Create emote entries
        final var used = new IntRBTreeSet();
        for (var entry : emoteAliases.entrySet()) {
            final var seq = entry.getKey();
            final var aliases = entry.getValue();
            final var emot = emoticons.get(seq);
            final var e = createEmote(
                    location, used, seq, aliases, emot == null ? null : emot.toArray(EMPTY_STRING_ARRAY)
            );

            instance.emotes.add(e);
            instance.emotesByUnicodeSequence.put(seq, e);
            final var modEmote = new UnicodeEmoteData.ModifiedEmote(e);
            for (String alias : aliases) instance.emotesByAlias.put(alias, modEmote);
            for (String emoticon : e.emoticons()) instance.emotesByEmoticon.put(emoticon, e);
        }
        // Modifiers
        if (json.has("modifiers")) {
            if (samples == null) {
                throw new JsonSyntaxException(
                        "Modifiers require a list of samples"
                );
            }
            final var modifiersJson = getAsJsonObject(json, "modifiers");
            final var modifiers = new ArrayList<UnicodeModifierType>(modifiersJson.size());
            final var modifierValues = new TreeMap<String, UnicodeModifierType.Modifier>();
            // Parse
            for (final Map.Entry<String, JsonElement> entry : modifiersJson.entrySet()) {
                final var name = entry.getKey();
                final var modifierJsonEl = entry.getValue();
                if (!modifierJsonEl.isJsonObject()) {
                    throw new JsonSyntaxException(
                            "Expected modifiers['" + name + "'] to be an object, got " +
                                    getType(modifierJsonEl)
                    );
                }
                final var modifierJson = modifierJsonEl.getAsJsonObject();
                if (!isObjectNode(modifierJson, "values")) {
                    throw new JsonSyntaxException(
                            "Expected modifiers['" + name + "'].values to be an object, got " +
                                    getType(modifierJson.get("values"))
                    );
                }
                final var valuesJson = modifierJson.getAsJsonObject("values");
                final var builder = UnicodeModifierType.build(name);
                if (modifierJson.has("priority")) {
                    final var prioJson = modifierJson.get("priority");
                    if (!isNumberValue(prioJson)) {
                        throw new JsonSyntaxException(
                                "Expected modifiers['" + name + "'].priority to be a number, got " +
                                        getType(prioJson)
                        );
                    }
                    builder.priority(prioJson.getAsInt());
                }
                if (modifierJson.has("postfix")) {
                    final var postfixJson = modifierJson.get("postfix");
                    if (isStringValue(postfixJson)) {
                        builder.postfix(postfixJson.getAsString());
                    } else {
                        throw new JsonSyntaxException(
                                "Expected modifiers['" + name + "'].postfix to be a string, got " +
                                        getType(postfixJson)
                        );
                    }
                }
                final int replaceLen;
                if (modifierJson.has("replace")) {
                    final var strings = parseNames(
                            modifierJson.get("replace"),
                            () -> "modifiers['" + name + "'].replace"
                    );
                    replaceLen = strings.length;
                    for (int i = 0; i < replaceLen; i++) {
                        try {
                            builder.replace(Pattern.compile(strings[i]));
                        } catch (PatternSyntaxException ex) {
                            throw new JsonSyntaxException(
                                    "Value modifiers['" + name + "'].replace[" + i + "] ('" +
                                            strings[i] + "') is not a valid regular expression",
                                    ex
                            );
                        }
                    }
                } else replaceLen = -1;
                final boolean zwj = getBooleanOptional(modifierJson.get("zwj"), () -> "modifiers['" + name + "'].zwj", false);
                final boolean vs16 = getBooleanOptional(modifierJson.get("vs16"), () -> "modifiers['" + name + "'].vs16", false);
                for (final Map.Entry<String, JsonElement> valueEntry : valuesJson.entrySet()) {
                    final var key = valueEntry.getKey();
                    final var seq = new StringBuilder();
                    if (zwj) seq.append(ZWJ);
                    if (SINGLE_CODE_POINT_PATTERN.matcher(key).matches()) {
                        // Literal code point
                        seq.append(Character.toChars(Integer.parseInt(key, 16)));
                    } else {
                        // Reference to alias
                        final var ref = allAliases.get(key);
                        if (ref == null) {
                            throw new JsonSyntaxException(
                                    "Key '" + key + "' in modifiers['" + name + "'].values is not an emote alias"
                            );
                        }
                        seq.append(ref);
                    }
                    if (vs16) seq.append(VS16);
                    final String[] names = parseNames(
                            valueEntry.getValue(),
                            () -> "modifiers['" + name + "'].values['" + key + "']"
                    );
                    for (int i = 0; i < names.length; i++) {
                        final var mName = names[i];
                        if (builder.isNameUsed(mName) || modifierValues.containsKey(mName)) {
                            throw new JsonSyntaxException(
                                    "Value modifiers['" + name + "'].values['" + key + "'][" + i + "] ('" +
                                            names[i] + "') is not unique in this modifier"
                            );
                        }
                    }
                    if (replaceLen >= 0 && names.length != replaceLen) {
                        throw new JsonSyntaxException(
                                "Length of modifiers['" + name + "'].values['" + key +
                                        "'] does not match the length of modifiers['" + name + "'].replace"
                        );
                    }
                    builder.addVariant(seq.toString(), names);
                }
                final var mod = builder.create();
                for (UnicodeModifierType.Modifier value : mod.values()) {
                    modifierValues.put(value.sequence, value);
                }
                modifiers.add(mod);
            }
            // Apply
            for (String sample : samples) {
                int i = Integer.MAX_VALUE;
                int modLen = 0;
                UnicodeModifierType.Modifier mod = null;
                for (final var mvEntry : modifierValues.entrySet()) {
                    final var name = mvEntry.getKey();
                    final int idx = sample.indexOf(name, 1);
                    if (idx != -1 && idx < i) {
                        mod = mvEntry.getValue();
                        modLen = name.length();
                        i = idx;
                    }
                }
                if (i == Integer.MAX_VALUE) continue;
                assert mod != null;
                final var mods = new ArrayList<UnicodeModifierType.Modifier>(1);
                final var sb = new StringBuilder(sample.length() - modLen);
                int clip = 0;
                do {
                    mods.add(mod);
                    sb.append(sample, clip, i);
                    clip = i + modLen;
                    if (clip == sample.length()) break;
                    i = Integer.MAX_VALUE;
                    for (final var mvEntry : modifierValues.entrySet()) {
                        final var name = mvEntry.getKey();
                        final int idx = sample.indexOf(name, clip);
                        if (idx != -1 && idx < i) {
                            mod = mvEntry.getValue();
                            modLen = name.length();
                            i = idx;
                        }
                    }
                } while (i != Integer.MAX_VALUE);
                sb.append(sample, clip, sample.length());
                // Add modified emote
                final Emote baseEmote;
                {
                    var baseSeq = sb.toString();
                    if (baseSeq.codePointCount(0, baseSeq.length()) == 2
                            && baseSeq.charAt(baseSeq.length() - 1) == VS16) {
                        // Text mode emoji
                        baseSeq = baseSeq.substring(0, baseSeq.length() - 1);
                    }
                    baseEmote = instance.emoteForUnicodeSequence(baseSeq);
                }
                if (baseEmote != null) {
                    final var aliasArray = new ArrayList<String>(baseEmote.aliases().length);
                    // transform aliases
                    for (String alias : baseEmote.aliases()) {
                        boolean modified = false;
                        for (UnicodeModifierType.Modifier m : mods) {
                            final var replace = m.getType().replace();
                            if (replace == null) continue;
                            for (int j = 0; j < replace.length; j++) {
                                final Pattern pattern = replace[j];
                                final var matcher = pattern.matcher(alias);
                                if (matcher.find()) {
                                    alias = matcher.replaceFirst(m.names()[j]);
                                    modified = true;
                                    break;
                                }
                            }
                        }
                        if (modified) aliasArray.add(alias);
                    }
                    final var newAliases = aliasArray.isEmpty()
                            ? baseEmote.aliases()
                            : aliasArray.toArray(EMPTY_STRING_ARRAY);
                    final var e = createEmote(
                            location, used, sample, newAliases, null
                    );

                    instance.emotes.add(e);
                    if (!aliasArray.isEmpty()) {
                        final var modEmote = new UnicodeEmoteData.ModifiedEmote(e);
                        for (var alias : aliasArray) instance.emotesByAlias.put(alias, modEmote);
                    }
                    instance.emotesByUnicodeSequence.put(sample, e);
                    if (baseEmote.aliases().length > 0) {
                        final var modEmote = instance.emotesByAlias.get(baseEmote.aliases()[0]);
                        final var key = new UnicodeEmoteData.Modifiers(mods.toArray(new UnicodeModifierType.Modifier[0]));
                        modEmote.modifications().put(key, e);
                    }
                }
            }
        }
        return instance;
    }

    private static void addCodePoint(
            @Nullable Set<String> samples,
            HashMap<String, String> allAliases,
            TreeMap<String, String[]> emoteAliases,
            int c,
            String[] aliases
    ) {
        final var seq = Character.toString(c);
        if (samples != null) {
            samples.remove(seq);
        }
        for (String alias : aliases) {
            allAliases.put(alias, seq);
        }
        emoteAliases.put(seq, aliases);
    }

    private static boolean getBooleanOptional(
            final JsonElement json,
            final Supplier<String> path,
            boolean def
    ) {
        if (json == null) return def;
        if (isBooleanValue(json)) {
            return json.getAsJsonPrimitive().getAsBoolean();
        } else {
            throw new JsonSyntaxException(
                    "Expected " + path.get() + " to be a boolean, got " + getType(json)
            );
        }
    }

    private static @NotNull String @NotNull [] parseAliases(
            final JsonElement json,
            final String key
    ) throws JsonSyntaxException {
        return parseNames(json, () -> "symbols['" + key + "']");
    }

    private static @NotNull String @NotNull [] parseAliases(
            final @NotNull JsonElement json,
            final @NotNull String key,
            final int subKey
    ) throws JsonSyntaxException {
        return parseNames(json, () -> "symbols['" + key + "'][" + subKey + "]");
    }

    private static @NotNull String @NotNull [] parseEmoticons(
            final @NotNull JsonElement json,
            final String key
    ) throws JsonSyntaxException {
        return parseNames(json, () -> "emoticons[" + key + "]");
    }

    private static @NotNull String @NotNull [] parseNames(
            final JsonElement json,
            final Supplier<String> path
    ) throws JsonSyntaxException {
        if (isStringValue(json)) {
            return new String[]{json.getAsString()};
        }
        if (json.isJsonArray()) {
            final var array = json.getAsJsonArray();
            final var aliases = new String[array.size()];
            for (int i = 0; i < aliases.length; i++) {
                final var alias = array.get(i);
                if (!isStringValue(alias)) {
                    throw new JsonSyntaxException(
                            "Expected " + path.get() +
                                    "[" + i + "] to be a string, was " +
                                    getType(alias)
                    );
                }
                aliases[i] = alias.getAsString();
            }
            return aliases;
        }
        throw new JsonSyntaxException(
                "Expected " + path.get() +
                        " to be a string or array, was " +
                        getType(json)
        );
    }

    private void resolvePatternEntry(
            final @NotNull String key,
            final @NotNull UnicodePattern pattern,
            final @NotNull JsonElement json,
            final @Nullable Set<String> samples,
            final @NotNull Map<String, String> allAliases,
            final @NotNull Map<String, String[]> emoteAliases
    ) throws JsonSyntaxException {
        // TODO: Optimize for single match
        if (samples == null) {
            throw new JsonSyntaxException(
                    "Patterns require a list of samples"
            );
        }
        final UnicodeJoiner[] joiners;
        try {
            if (json.isJsonArray()) {
                final var array = json.getAsJsonArray();
                joiners = new UnicodeJoiner[array.size()];
                for (int i = 0; i < joiners.length; i++) {
                    final var entry = array.get(i);
                    if (isStringValue(entry)) {
                        final var j = UnicodeJoiner.parse(entry.getAsString());
                        joiners[i] = j;
                        if (!j.isCompatibleWith(pattern)) {
                            throw new JsonSyntaxException(
                                    "Joiner '" + json.getAsString() + "' is incompatible with pattern '" + key + "'"
                            );
                        }
                    } else {
                        throw new JsonSyntaxException(
                                "Expected symbols['" + key + "'][" + i + "] to be a string, was " +
                                        getType(entry)
                        );
                    }
                }
            } else if (isStringValue(json)) {
                joiners = new UnicodeJoiner[1];
                final var j = UnicodeJoiner.parse(json.getAsString());
                joiners[0] = j;
                if (!j.isCompatibleWith(pattern)) {
                    throw new JsonSyntaxException(
                            "Joiner '" + json.getAsString() + "' is incompatible with pattern '" + key + "'"
                    );
                }
            } else {
                throw new JsonSyntaxException(
                        "Expected symbols['" + key + "'] to be a string or array, was " +
                                getType(json)
                );
            }
        } catch (ParseException ex) {
            throw new JsonSyntaxException(
                    "Invalid unicode joiner in symbols['" + key + "']",
                    ex
            );
        }
        // Find matches
        final var toRemove = new ArrayList<String>();
        for (final var sample : samples) {
            final var result = pattern.apply(sample);
            if (result != null) {
                final String[] aliases = new String[joiners.length];
                for (int i = 0; i < joiners.length; i++) {
                    aliases[i] = joiners[i].evaluate(result);
                }
                for (String alias : aliases) {
                    allAliases.put(alias, sample);
                }
                emoteAliases.put(sample, aliases);
                toRemove.add(sample);
            }
        }
        toRemove.forEach(samples::remove);
    }

    private static final Int2ObjectMap<ResourceLocation> FONTS = new Int2ObjectOpenHashMap<>(8);

    private @NotNull Emote createEmote(
            final @NotNull ResourceLocation base,
            final @NotNull IntRBTreeSet used,
            final @NotNull String seq,
            final @NotNull String @NotNull [] aliases,
            @NotNull String @Nullable [] emoticons
    ) {
        if (seq.length() == 0) throw new IllegalArgumentException("Empty emote unicode sequence");
        int cp = seq.codePointAt(0);
        if (used.contains(cp)) {
            cp += 100 << 16;
            final var usedIt = used.tailSet(cp).iterator();
            while (usedIt.hasNext() && usedIt.nextInt() == cp) cp++;
            if (used.contains(cp)) throw new AssertionError();
        }
        used.add(cp);
        final var font = getFont(base, cp >> 16);
        final char ch = (char) (cp & 0xffff);
        if (emoticons == null) emoticons = EMPTY_STRING_ARRAY;
        return new Emote(font, ch, aliases, emoticons, seq);
    }

    private @NotNull ResourceLocation getFont(ResourceLocation base, int i) {
        final var existing = FONTS.get(i);
        if (existing != null) return existing;
        final var rl = new ResourceLocation(base.getNamespace(), base.getPath() + i);
        FONTS.put(i, rl);
        return rl;
    }
}
