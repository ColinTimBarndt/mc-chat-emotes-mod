package io.github.colintimbarndt.chat_emotes.config;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod;
import io.github.colintimbarndt.chat_emotes.util.BomAwareReader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class ChatEmotesConfig {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourcepackConfig.LIST_TYPE, new ResourcepackConfig.ListSerializer())
            .registerTypeAdapter(ChatEmotesConfig.class, new Serializer())
            .create();

    private final @NotNull List<ResourcepackConfig> resourcepacks;

    public ChatEmotesConfig() {
        this(Collections.emptyList());
    }

    private ChatEmotesConfig(@NotNull List<ResourcepackConfig> resourcepacks) {
        this.resourcepacks = resourcepacks;
    }

    public @NotNull List<ResourcepackConfig> getResourcepacks() {
        return resourcepacks;
    }

    public void save(OutputStream output) throws IOException {
        try (final var writer = new JsonWriter(new OutputStreamWriter(output))) {
            GSON.toJson(this, ChatEmotesConfig.class, writer);
        }
    }

    public static ChatEmotesConfig load(InputStream input) throws IOException, JsonParseException {
        try (final var reader = BomAwareReader.create(input)) {
            return GSON.fromJson(reader, ChatEmotesConfig.class);
        }
    }

    public static class Serializer implements JsonSerializer<ChatEmotesConfig>, JsonDeserializer<ChatEmotesConfig> {
        @Override
        public JsonElement serialize(ChatEmotesConfig config, Type type, JsonSerializationContext ctx) {
            final var root = new JsonObject();
            root.addProperty("version", ChatEmotesMod.getModMetadata().getVersion().getFriendlyString());
            root.add("resourcepacks", ctx.serialize(config.resourcepacks, ResourcepackConfig.LIST_TYPE));
            return root;
        }

        @Override
        public ChatEmotesConfig deserialize(
                JsonElement element,
                Type type,
                JsonDeserializationContext ctx
        ) throws JsonParseException {
            final var root = element.getAsJsonObject();
            if (!GsonHelper.isStringValue(root, "version")) {
                throw new JsonSyntaxException(
                        "Expected version to be a string, got " + GsonHelper.getType(root.get("version"))
                );
            }
            final Version version;
            try {
                version = Version.parse(GsonHelper.getAsString(root, "version"));
            } catch (VersionParsingException ex) {
                throw new JsonSyntaxException("Invalid version", ex);
            }
            if (version.compareTo(ChatEmotesMod.getModMetadata().getVersion()) != 0) {
                throw new JsonParseException("Unsupported version of config: " + version.getFriendlyString());
            }
            final List<ResourcepackConfig> resourcepacks = ctx.deserialize(
                    GsonHelper.getAsJsonObject(root, "resourcepacks"),
                    ResourcepackConfig.LIST_TYPE
            );
            return new ChatEmotesConfig(resourcepacks);
        }
    }
}
