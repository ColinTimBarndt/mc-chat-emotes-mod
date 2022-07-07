package io.github.colintimbarndt.chat_emotes;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ChatEmotesConfig {

    private static final Predicate<String> ASCII_TEST =
            Pattern.compile("^[a-z\\d]+$").asMatchPredicate();

    private final @NotNull Identifier font;
    private final @NotNull Map<String, Emote> emotes;
    private final @NotNull Map<String, Emote> emoteAliases = new HashMap<>();
    private final @NotNull Set<String> specialEmotes = new HashSet<>();

    public ChatEmotesConfig(
            @NotNull Identifier font,
            @NotNull Map<String, Emote> emotes
    ) {
        this.font = font;
        this.emotes = emotes;
        for (var emote : emotes.values()) {
            emoteAliases.put(emote.name, emote);
            for (var alt : emote.alt) {
                emoteAliases.put(alt, emote);
                if (!ASCII_TEST.test(alt))
                    specialEmotes.add(alt);
            }
        }
    }

    public static final ChatEmotesConfig DEFAULT = new ChatEmotesConfig(
            new Identifier("chat-emotes", "1"),
            Collections.emptyMap()
    );

    public static ChatEmotesConfig getDefault() {
        return new ChatEmotesConfig(
                DEFAULT.font, new HashMap<>()
        );
    }

    public static ChatEmotesConfig load() throws IOException {
        final var file = FabricLoader.getInstance().getConfigDir().resolve("chat-emotes.json").toFile();
        final var gson = new GsonBuilder()
                .registerTypeAdapter(ChatEmotesConfig.class, new ConfigSerializer())
                .setPrettyPrinting()
                .create();
        if (!file.isFile()) {
            if (file.exists()) {
                ChatEmotesMod.LOGGER.error("Unable to load config: Exists, but is not a file");
                return getDefault();
            }
            if (file.createNewFile()) {
                final var writer = new FileWriter(file);
                writer.write(gson.toJson(DEFAULT));
                writer.flush();
                writer.close();
            }
            return getDefault();
        }
        return gson.fromJson(new FileReader(file), ChatEmotesConfig.class);
    }

    public @NotNull Identifier font() {
        return font;
    }

    public int emotes() {
        return emotes.size();
    }

    public boolean isSpecialEmote(String s) {
        return specialEmotes.contains(s);
    }

    public Emote getEmote(@NotNull String alias) {
        return emoteAliases.get(alias);
    }

    public record Emote(
            @NotNull String name,
            @NotNull String character,
            @Nullable String permission,
            @NotNull Set<String> alt
            ) {}

    private static class ConfigSerializer implements JsonSerializer<ChatEmotesConfig>, JsonDeserializer<ChatEmotesConfig> {

        @Override
        public JsonElement serialize(ChatEmotesConfig config, Type type, JsonSerializationContext ctx) {
            final var obj = new JsonObject();
            final var emotes = new JsonArray();
            obj.addProperty("font", config.font.toString());
            obj.add("emotes", emotes);
            JsonObject eobj;
            for (var e : config.emotes.values()) {
                eobj = new JsonObject();
                eobj.addProperty("name", e.name);
                eobj.addProperty("char", e.character);
                if (e.permission != null)
                    eobj.addProperty("permission", e.permission);
                if (e.alt.size() > 0)
                    eobj.add("alt", ctx.serialize(e.alt));
                emotes.add(eobj);
            }
            return obj;
        }

        @Override
        public ChatEmotesConfig deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            try {
                final var obj = element.getAsJsonObject();

                var font = obj.get("font");
                var emotes = obj.get("emotes");

                if (font != null && !font.isJsonPrimitive())
                    throw new JsonParseException("Invalid Config: font is not a string");
                if (emotes != null && !emotes.isJsonArray())
                    throw new JsonParseException("Invalid Config: invisibleFont is not an array");

                final Map<String, Emote> emap;
                if (emotes == null) {
                    emap = new HashMap<>(0);
                } else {
                    final var emotesArray = emotes.getAsJsonArray();
                    emap = new HashMap<>(emotesArray.size());
                    for (var eel : emotesArray) {
                        final var eobj = eel.getAsJsonObject();

                        final var name = eobj.get("name");
                        final var character = eobj.get("char");
                        final var permission = eobj.get("permission");
                        final var alt = eobj.get("alt");
                        final HashSet<String> altSet;

                        if (name == null || !name.isJsonPrimitive())
                            throw new JsonParseException("Invalid Config: emotes[].name is not a string");
                        if (character == null || !character.isJsonPrimitive())
                            throw new JsonParseException("Invalid Config: emotes[].char is not a string");
                        if (permission != null && !permission.isJsonPrimitive())
                            throw new JsonParseException("Invalid Config: emotes[].permission is not a string");
                        if (alt == null) {
                            altSet = new HashSet<>(0);
                        } else if (alt.isJsonArray()) {
                            final var arr = alt.getAsJsonArray();
                            altSet = new HashSet<>(arr.size());
                            for (var s : arr)
                                altSet.add(s.getAsString());
                        } else {
                            throw new JsonParseException("Invalid Config: emotes[].alt is not an array");
                        }

                        final var nameString = name.getAsString();
                        final var permString = permission == null ? null : permission.getAsString();

                        emap.put(nameString, new Emote(nameString, character.getAsString(), permString, altSet));
                    }
                }


                return new ChatEmotesConfig(
                        font == null ? DEFAULT.font : new Identifier(font.getAsString()),
                        emap
                );
            } catch(IllegalStateException | InvalidIdentifierException ex) {
                throw new JsonParseException("Invalid Config", ex);
            }
        }
    }
}
