package io.github.colintimbarndt.chat_emotes.util;

import com.google.gson.stream.JsonWriter;
import com.mojang.bridge.game.PackType;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackWriter implements Closeable, AutoCloseable {
    private final ZipOutputStream zipStream;
    private final PackType packType;
    public PackWriter(File file, PackType packType) throws IOException {
        if (!(file.exists() || file.createNewFile())) {
            throw new IOException("Unable to create file");
        }
        zipStream = new ZipOutputStream(new FileOutputStream(file));
        zipStream.setMethod(ZipOutputStream.DEFLATED);
        this.packType = packType;
    }

    public void write(
            @NotNull String name,
            final @NotNull InputStream inStream
    ) throws IOException {
        zipStream.putNextEntry(new ZipEntry(name));
        final var stream = new PackEntryOutputStream();
        inStream.transferTo(stream);
        stream.close();
    }

    public void write(
            @NotNull String name,
            final @NotNull ThrowingConsumer<OutputStream, IOException> writer
    ) throws IOException {
        zipStream.putNextEntry(new ZipEntry(name));
        final var stream = new PackEntryOutputStream();
        writer.accept(stream);
        stream.close();
    }

    public void write(@NotNull PackMeta meta) throws IOException {
        meta.version = SharedConstants.getCurrentVersion().getPackVersion(packType);
        write("pack.mcmeta", meta::write);
    }

    public void write(
            @NotNull ResourceLocation loc,
            final @NotNull ThrowingConsumer<OutputStream, IOException> writer
    ) throws IOException {
        final String path =
                (packType == PackType.DATA ? "data" : "assets") +
                '/' +
                loc.getNamespace() +
                '/' +
                loc.getPath();
        write(path, writer);
    }

    @Override
    public void close() throws IOException {
        zipStream.finish();
        zipStream.flush();
        zipStream.close();
    }

    public static final class PackMeta {
        private Component description = Component.empty();
        private int version = -1;

        public PackMeta description(@NotNull Component description) {
            this.description = description;
            return this;
        }
        public PackMeta description(@NotNull String description) {
            this.description = Component.nullToEmpty(description);
            return this;
        }

        public void write(OutputStream out) throws IOException {
            try(final var writer = new JsonWriter(new OutputStreamWriter(out))) {
                writer.setIndent("  ");
                writer.beginObject();
                writer.name("pack").beginObject();
                writer.name("pack_format").value(version);
                writer.name("description");
                writer.jsonValue(Component.Serializer.toJson(description));
                writer.endObject();
                writer.endObject();
            }
        }
    }

    public final class PackEntryOutputStream extends OutputStream {
        private boolean closed = false;
        private PackEntryOutputStream() {
            super();
        }

        @Override
        public void write(int i) throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
            zipStream.write(i);
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                zipStream.closeEntry();
            }
        }
    }
}
