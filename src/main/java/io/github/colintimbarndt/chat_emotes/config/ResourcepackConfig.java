package io.github.colintimbarndt.chat_emotes.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.mojang.bridge.game.PackType;
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod;
import io.github.colintimbarndt.chat_emotes.data.EmoteData;
import io.github.colintimbarndt.chat_emotes.data.FontGenerator;
import io.github.colintimbarndt.chat_emotes.data.PackExportException;
import io.github.colintimbarndt.chat_emotes.util.PackWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

import static io.github.colintimbarndt.chat_emotes.ChatEmotesMod.MOD_ID;

public record ResourcepackConfig(
        String name,
        Map<ResourceLocation, Path> resourceFiles
) {
    @SuppressWarnings("UnstableApiUsage")
    public static final Type LIST_TYPE = new TypeToken<List<ResourcepackConfig>>(){}.getType();

    public void export(@NotNull List<EmoteData> emoteData) throws IOException, PackExportException {
        final var filteredData = emoteData.stream()
                .filter(data -> resourceFiles.containsKey(data.getLocation())).toList();
        if (filteredData.isEmpty()) {
            throw new PackExportException("No emotes to export");
        }
        final var configPath = ChatEmotesMod.getConfigDir();
        final var exportPath = configPath.resolve("export");
        {
            final var exportFile = exportPath.toFile();
            if (!(exportFile.exists() || exportFile.mkdirs())) {
                throw new IOException("Unable to create export directory");
            }
        }
        try (final var writer = new PackWriter(exportPath.resolve(name + ".zip").toFile(), PackType.RESOURCE)) {
            writer.write(new PackWriter.PackMeta().description(
                    Component.literal(name + " Emote Pack")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
            ));
            {
                // Write pack.png
                final var iconOpt = ChatEmotesMod.getModMetadata().getIconPath(128);
                if (iconOpt.isPresent()) {
                    var icon = iconOpt.get();
                    if (!icon.startsWith("/")) icon = "/" + icon;
                    try (final var iconStream = ChatEmotesMod.class.getResourceAsStream(icon)) {
                        if (iconStream == null) {
                            throw new IOException("Unable to load icon");
                        }
                        writer.write("pack.png", iconStream);
                    }
                }
            }
            Translations: {
                // Write translations
                final var mfis = ChatEmotesMod.class.getResourceAsStream("/assets/" + MOD_ID + "/lang/MANIFEST");
                if (mfis == null) break Translations;
                try (final var manifest = new BufferedReader(new InputStreamReader(mfis))) {
                    final var lines = manifest.lines().iterator();
                    while (lines.hasNext()) {
                        final var code = lines.next();
                        if (code.isEmpty()) continue;
                        final var lang = ChatEmotesMod.class.getResourceAsStream("/assets/" + MOD_ID + "/lang/" + code + ".json");
                        if (lang != null) {
                            writer.write("assets/" + MOD_ID + "/lang/" + code + ".json", lang);
                        }
                    }
                }
            }
            final var fontGen = new FontGenerator(16, writer);
            for (EmoteData data : filteredData) {
                data.generateFonts(fontGen, configPath.resolve(resourceFiles.get(data.getLocation())));
            }
        }
    }

    public static class ListSerializer
            implements JsonSerializer<List<ResourcepackConfig>>, JsonDeserializer<List<ResourcepackConfig>>
    {
        @Override
        public JsonElement serialize(
                List<ResourcepackConfig> resourcepackConfigs,
                Type type,
                JsonSerializationContext jsonSerializationContext)
        {
            final var root = new JsonObject();
            for (var config : resourcepackConfigs) {
                final var configRoot = new JsonObject();
                for (Map.Entry<ResourceLocation, Path> entry : config.resourceFiles.entrySet()) {
                    configRoot.addProperty(entry.getKey().toString(), entry.getValue().toString());
                }
                root.add(config.name, configRoot);
            }
            return root;
        }

        @Override
        public List<ResourcepackConfig> deserialize(
                JsonElement jsonElement,
                Type type,
                JsonDeserializationContext jsonDeserializationContext
        ) throws JsonParseException {
            final var root = jsonElement.getAsJsonObject();
            final var configs = new ArrayList<ResourcepackConfig>(root.size());
            for (final var entry : root.entrySet()) {
                final var filesRoot = entry.getValue().getAsJsonObject();
                final var files = new HashMap<ResourceLocation, Path>(filesRoot.size());
                for (Map.Entry<String, JsonElement> fileEntry : filesRoot.entrySet()) {
                    final ResourceLocation location;
                    final Path path;
                    try {
                        location = new ResourceLocation(fileEntry.getKey());
                    } catch(ResourceLocationException ex) {
                        throw new JsonSyntaxException(
                                "Invalid resource location '" + fileEntry.getKey() +
                                        "' in resourcepacks['" + entry.getKey() + "']",
                                ex
                        );
                    }
                    final var val = fileEntry.getValue();
                    if (!GsonHelper.isStringValue(val)) {
                        throw new JsonSyntaxException(
                                "Expected resourcepacks['" + entry.getKey() + "']['" + fileEntry.getKey() + "'] " +
                                        "to be a string, got " + GsonHelper.getType(val)
                        );
                    }
                    path = Path.of(val.getAsString());
                    files.put(location, path);
                }
                configs.add(new ResourcepackConfig(entry.getKey(), Collections.unmodifiableMap(files)));
            }
            return configs;
        }
    }
}
