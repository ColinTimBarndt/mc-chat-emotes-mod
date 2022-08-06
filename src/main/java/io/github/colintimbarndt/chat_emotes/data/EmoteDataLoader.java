package io.github.colintimbarndt.chat_emotes.data;

import com.google.gson.*;
import io.github.colintimbarndt.chat_emotes.util.BomAwareReader;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.github.colintimbarndt.chat_emotes.ChatEmotesMod.*;

public final class EmoteDataLoader implements SimpleResourceReloadListener<List<EmoteData>> {
    private static final ResourceLocation ID = new ResourceLocation(MOD_ID, "emotes");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .create();
    private @UnmodifiableView List<EmoteData> loadedEmoteData = Collections.emptyList();

    public @UnmodifiableView List<EmoteData> getLoadedEmoteData() {
        return loadedEmoteData;
    }

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public CompletableFuture<List<EmoteData>> load(
            @NotNull ResourceManager manager,
            ProfilerFiller profiler,
            Executor executor
    ) {
        final var resources = manager.listResources("emote", loc -> loc.getPath().endsWith(".json"));
        final var samples = manager.listResources("emote", loc -> loc.getPath().endsWith(".txt"));
        final var dataList = new ArrayList<EmoteData>(resources.size());
        for (var entry : resources.entrySet()) {
            final var id = entry.getKey();
            try {
                final var json = GSON.fromJson(entry.getValue().openAsReader(), JsonObject.class);
                final var type = GSON.fromJson(json.get("type"), ResourceLocation.class);
                final var serializer = EMOTE_DATA_SERIALIZER.get(type);
                if (serializer == null) {
                    LOGGER.warn("Unknown emote data type {}", type);
                    continue;
                }
                final Resource samplesResource;
                final ResourceLocation samplesResourceLocation;
                {
                    final String path = id.getPath();
                    final var loc = new ResourceLocation(
                            id.getNamespace(),
                            path.substring(0, path.length() - 5) + ".txt"
                    );
                    samplesResource = samples.get(loc);
                    samplesResourceLocation = loc;
                }
                Set<String> sampleStrings = null;
                if (samplesResource != null) {
                    try {
                        sampleStrings = readSamples(samplesResource);
                    } catch (IOException ex) {
                        LOGGER.error("Unable to load resource {}", samplesResourceLocation, ex);
                    }
                }
                final ResourceLocation emoteId;
                {
                    final String path = id.getPath();
                    emoteId = new ResourceLocation(
                            id.getNamespace(),
                            path.substring(6, path.length() - 5)
                    );
                }
                final var eData = serializer.read(emoteId, json, sampleStrings);
                dataList.add(eData);
            } catch (IOException | JsonIOException | JsonSyntaxException ex) {
                LOGGER.error("Unable to load resource {}", id, ex);
            }
        }
        return CompletableFuture.completedFuture(dataList);
    }

    @Override
    public CompletableFuture<Void> apply(
            List<EmoteData> data,
            ResourceManager manager,
            ProfilerFiller profiler,
            Executor executor
    ) {
        loadedEmoteData = Collections.unmodifiableList(data);
        LOGGER.info("Loaded {} emotes", data.stream().mapToInt(d -> d.getEmotes().size()).sum());
        return CompletableFuture.completedFuture(null);
    }

    private @NotNull Set<String> readSamples(Resource resource) throws IOException {
        final var results = new HashSet<String>();
        try (final var stream = BomAwareReader.createBuffered(resource.open(), 64)) {
            final var lines = stream.lines().iterator();
            while (lines.hasNext()) {
                final var line = lines.next();
                if (!line.isEmpty()) {
                    results.add(line);
                }
            }
        }
        return results;
    }
}
