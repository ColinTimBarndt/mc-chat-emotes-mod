package io.github.colintimbarndt.chat_emotes.data;

import com.google.gson.stream.JsonWriter;
import io.github.colintimbarndt.chat_emotes.util.PackWriter;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public final class FontGenerator {
    private final int atlasSprites;
    private final PackWriter writer;
    public FontGenerator(
            int atlasSprites,
            PackWriter writer
    ) {
        this.atlasSprites = atlasSprites;
        this.writer = writer;
    }

    public Font createFont(ResourceLocation id, int textureSize) {
        return new Font(id, textureSize);
    }

    public final class Font implements Closeable, AutoCloseable {
        private final List<FontTexture> textures = new ArrayList<>();
        private FontTexture currentTexture = null;
        private final ResourceLocation id;
        private final int spriteSize;
        private int textureIndex = Integer.MAX_VALUE;
        private boolean finished = false;
        private Font(ResourceLocation id, int spriteSize) {
            this.id = new ResourceLocation(id.getNamespace(), "font/" + id.getPath());
            this.spriteSize = spriteSize;
        }
        public void addSprite(BufferedImage sprite, char character) throws IOException {
            if (finished) throw new IllegalStateException("Already finished this font");
            if (sprite.getWidth() != spriteSize
                    || sprite.getHeight() != spriteSize) {
                throw new IllegalArgumentException("Invalid image size");
            }
            if (textureIndex >= atlasSprites * atlasSprites) {
                if (currentTexture != null) {
                    currentTexture.close();
                }
                currentTexture = new FontTexture(getTextureLocation(textures.size()), spriteSize);
                textures.add(currentTexture);
                currentTexture.graphics.drawImage(sprite, 0, 0, spriteSize, spriteSize, null);
                currentTexture.characters[0] = character;
                textureIndex = 1;
            } else {
                final int x = (textureIndex % atlasSprites) * spriteSize;
                final int y = (textureIndex / atlasSprites) * spriteSize;
                currentTexture.graphics.drawImage(sprite, x, y, spriteSize, spriteSize, null);
                currentTexture.characters[textureIndex] = character;
                textureIndex++;
            }
        }

        @Override
        public void close() throws IOException {
            if (finished) throw new IllegalStateException("Already finished this font");
            finished = true;
            if (currentTexture != null) {
                currentTexture.close();
                currentTexture = null;
            }
            writer.write(
                    new ResourceLocation(id.getNamespace(), id.getPath() + ".json"),
                    this::writeFontJson
            );
            textures.clear();
        }
        private ResourceLocation getTextureLocation(int i) {
            final String path = id.getPath();
            return new ResourceLocation(id.getNamespace(), "textures/" + path + "_" + i + ".png");
        }
        private void writeFontJson(OutputStream stream) throws IOException {
            try (final var writer = new JsonWriter(new OutputStreamWriter(stream))) {
                writer.setIndent("  ");
                writer.beginObject()
                        .name("providers").beginArray();
                for (final FontTexture texture : textures) {
                    writer.beginObject()
                            .name("type").value("bitmap")
                            .name("file").value(texture.getTextureLocation().toString())
                            .name("ascent").value(8)
                            .name("chars").beginArray();

                    for(int i = 0; i < texture.characters.length; i += atlasSprites) {
                        final StringBuilder sb = new StringBuilder(2 * atlasSprites);
                        for(int j = 0; j < atlasSprites; j++) {
                            sb.append(texture.characters[i + j]);
                        }
                        writer.value(sb.toString());
                    }

                    writer.endArray()
                            .endObject();
                }
                writer.endArray().endObject();
            }
        }
    }

    private final class FontTexture implements Closeable, AutoCloseable {
        private final char[] characters;
        private final ResourceLocation location;
        private BufferedImage image;
        private Graphics2D graphics;
        private FontTexture(ResourceLocation location, int textureSize) {
            this.location = location;
            this.characters = new char[atlasSprites * atlasSprites];
            textureSize *= atlasSprites;
            this.image = new BufferedImage(textureSize, textureSize, TYPE_INT_ARGB);
            this.graphics = image.createGraphics();
        }

        @Override
        public void close() throws IOException {
            if (graphics != null) {
                graphics.dispose();
                graphics = null;
                writer.write(location, stream -> ImageIO.write(image, "png", stream));
                image = null;
            }
        }

        public ResourceLocation getTextureLocation() {
            return new ResourceLocation(
                    location.getNamespace(),
                    location.getPath().substring("textures/".length())
            );
        }
    }
}
