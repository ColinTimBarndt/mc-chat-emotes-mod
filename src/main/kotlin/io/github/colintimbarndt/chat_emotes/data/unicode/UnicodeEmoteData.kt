package io.github.colintimbarndt.chat_emotes.data.unicode;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.colintimbarndt.chat_emotes.data.Emote;
import io.github.colintimbarndt.chat_emotes.data.EmoteData;
import io.github.colintimbarndt.chat_emotes.data.EmoteDataSerializer;
import io.github.colintimbarndt.chat_emotes.data.FontGenerator;
import io.github.colintimbarndt.chat_emotes.data.unicode.joiner.UnicodeJoiner;
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static net.minecraft.util.GsonHelper.*;

public final class UnicodeEmoteData implements EmoteData {
    public static final UnicodeEmoteDataSerializer SERIALIZER = new UnicodeEmoteDataSerializer();

    private final ResourceLocation location;
    private final Set<Emote> emotes = new HashSet<>();
    private final Map<String, Emote> emotesByUnicodeSequence = new TreeMap<>();
    private final Map<String, Emote> emotesByAlias = new HashMap<>();
    private final Map<String, Emote> emotesByEmoticon = new HashMap<>();

    private UnicodeEmoteData(ResourceLocation location) {
        this.location = location;
    }

    public @NotNull ResourceLocation getLocation() {
        return location;
    }

    @Override
    public @NotNull Set<Emote> getEmotes() {
        return Collections.unmodifiableSet(emotes);
    }

    @Override
    public @NotNull Set<String> getAliases() {
        return emotesByAlias.keySet();
    }

    @Override
    public @NotNull Set<String> getEmoticons() {
        return emotesByEmoticon.keySet();
    }

    @Override
    public @Nullable Emote emoteForUnicodeSequence(@NotNull String sequence) {
        return emotesByUnicodeSequence.get(sequence);
    }

    @Override
    public @Nullable Emote emoteForAlias(@NotNull String alias) {
        return emotesByAlias.get(alias);
    }

    @Override
    public @Nullable Emote emoteForEmoticon(@NotNull String emoticon) {
        return emotesByEmoticon.get(emoticon);
    }

    @Override
    public @NotNull UnicodeEmoteDataSerializer getSerializer() {
        return SERIALIZER;
    }

    @Override
    public void generateFonts(@NotNull FontGenerator gen, @NotNull Path imageSources) throws IOException {
        // TODO: optimize
        final var fonts = new HashMap<ResourceLocation, FontGenerator.Font>(8);
        try (final var images = new EmoteTextureArchive(imageSources.toFile())) {
            for (Emote emote : emotes) {
                final var texture = images.getTextureAsStream(emote.unicodeSequence());
                if (texture != null) {
                    final var img = ImageIO.read(texture);
                    final var fontId = emote.font();
                    final FontGenerator.Font font;
                    if (fonts.containsKey(fontId)) {
                        font = fonts.get(fontId);
                    } else {
                        font = gen.createFont(fontId, img.getWidth());
                        fonts.put(fontId, font);
                    }
                    font.addSprite(img, emote.character());
                }
            }
        } finally {
            for (FontGenerator.Font font : fonts.values()) {
                font.close();
            }
        }
    }

    public static final class UnicodeEmoteDataSerializer implements EmoteDataSerializer<UnicodeEmoteData> {
        private static final Pattern CODE_POINT_PATTERN = Pattern.compile(
                "(?<min>[\\da-f]{1,6})(-(?<max>[\\da-f]{1,6}))?",
                Pattern.CASE_INSENSITIVE
        );
        private static final String[] EMPTY_STRING_ARRAY = {};
        private UnicodeEmoteDataSerializer() {}

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
                        final var seq = Character.toString(min);
                        for (String alias : aliases) {
                            allAliases.put(alias, seq);
                        }
                        emoteAliases.put(seq, aliases);
                        continue;
                    }
                    // range
                    if (!value.isJsonArray()) {
                        throw new JsonSyntaxException(
                                "Expected symbols['" + key + "'] to be a JsonArray, was "
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
                        final var seq = Character.toString(c);
                        for (String alias : aliases) {
                            allAliases.put(alias, seq);
                        }
                        emoteAliases.put(seq, aliases);
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
                final var wrapper = new Object() { boolean hasAllRefs = true; };
                pattern.getNameReferences(ref -> wrapper.hasAllRefs &= allAliases.containsKey(ref) );
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
                        final var wrapper = new Object() { boolean hasAllRefs = true; };
                        pattern.getNameReferences(ref -> wrapper.hasAllRefs &= allAliases.containsKey(ref) );
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
                for (String alias : aliases) instance.emotesByAlias.put(alias, e);
                for (String emoticon : e.emoticons()) instance.emotesByEmoticon.put(emoticon, e);
            }
            return instance;
        }

        private static @NotNull String @NotNull[] parseAliases(
                final JsonElement json,
                final String key
        ) throws JsonSyntaxException {
            return parseNames(json, () -> "symbols['" + key + "']");
        }

        private static @NotNull String @NotNull[] parseAliases(
                final @NotNull JsonElement json,
                final @NotNull String key,
                final int subKey
        ) throws JsonSyntaxException {
            return parseNames(json, () -> "symbols['" + key + "'][" + subKey + "]");
        }

        private static @NotNull String @NotNull[] parseEmoticons(
                final @NotNull JsonElement json,
                final String key
        ) throws JsonSyntaxException {
            return parseNames(json, () -> "emoticons[" + key + "]");
        }

        private static @NotNull String @NotNull[] parseNames(
                final JsonElement json,
                final Supplier<String> path
                ) throws JsonSyntaxException {
            if (isStringValue(json)) {
                return new String[] {json.getAsString()};
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
                            " to be a string or JsonArray, was " +
                            getType(json)
            );
        }

        private void resolvePatternEntry(
                final @NotNull String key,
                final @NotNull UnicodePattern pattern,
                final @NotNull JsonElement json,
                final @Nullable Iterable<String> samples,
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
                            "Expected symbols['" + key + "'] to be a string or JsonArray, was " +
                                    getType(json)
                    );
                }
            } catch(ParseException ex) {
                throw new JsonSyntaxException(
                        "Invalid unicode joiner in symbols['" + key + "']",
                        ex
                );
            }
            // Find matches
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
                }
            }
        }

        private static final Int2ObjectMap<ResourceLocation> FONTS = new Int2ObjectOpenHashMap<>(8);

        private @NotNull Emote createEmote(
                final @NotNull ResourceLocation base,
                final @NotNull IntRBTreeSet used,
                final @NotNull String seq,
                final @NotNull String @NotNull[] aliases,
                @NotNull String @Nullable[] emoticons
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
}
