package io.github.colintimbarndt.chat_emotes.data.unicode;

import io.github.colintimbarndt.chat_emotes.data.Emote;
import io.github.colintimbarndt.chat_emotes.data.EmoteData;
import io.github.colintimbarndt.chat_emotes.data.FontGenerator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class UnicodeEmoteData implements EmoteData {
    public static final UnicodeEmoteDataSerializer SERIALIZER = new UnicodeEmoteDataSerializer();

    private final ResourceLocation location;
    final Set<Emote> emotes = new HashSet<>();
    final Map<String, Emote> emotesByUnicodeSequence = new TreeMap<>();
    final Map<String, ModifiedEmote> emotesByAlias = new HashMap<>();
    final Map<String, Emote> emotesByEmoticon = new HashMap<>();

    UnicodeEmoteData(ResourceLocation location) {
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
        final var mEmote = emotesByAlias.get(alias);
        if (mEmote == null) return null;
        // TODO: Parse modifiers
        return mEmote.plain;
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
    record ModifiedEmote(Emote plain, Map<Modifiers, Emote> modifications) {
        public ModifiedEmote(Emote plain) {
            this(plain, new HashMap<>());
        }
    }
    record Modifiers(UnicodeModifierType.Modifier @NotNull[] modifiers) {
        public static final Modifiers NO_MODIFIERS = new Modifiers(new UnicodeModifierType.Modifier[0]);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Modifiers mods = (Modifiers) o;

            if (modifiers.length != mods.modifiers.length) return false;
            for (int i = 0; i < modifiers.length; i++) {
                if (modifiers[i] != mods.modifiers[i]) return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(modifiers);
        }
    }
}
