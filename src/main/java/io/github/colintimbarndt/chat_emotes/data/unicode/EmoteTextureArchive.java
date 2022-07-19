package io.github.colintimbarndt.chat_emotes.data.unicode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class EmoteTextureArchive extends ZipFile {
    public static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^[\\da-z]{1,6}((\\s+|[_-])[\\da-z]{1,6})*\\.png$",
            Pattern.CASE_INSENSITIVE
    );
    public static final Pattern FILENAME_SEPARATOR_PATTERN = Pattern.compile("\\s+|[_-]");

    private final TreeMap<String, ZipEntry> textures = new TreeMap<>();

    public EmoteTextureArchive(@NotNull File file) throws IOException {
        super(file, ZipFile.OPEN_READ);
        final var entries = entries().asIterator();
        while (entries.hasNext()) {
            final var entry = entries.next();
            if (!FILENAME_PATTERN.matcher(entry.getName()).matches()) continue;
            final StringBuilder seq = new StringBuilder(32);
            final var entryName = entry.getName();
            FILENAME_SEPARATOR_PATTERN.splitAsStream(entryName.substring(0, entryName.length() - 4))
                    .forEach(s -> {
                        final var cp = Integer.parseInt(s, 16);
                        if (Character.isBmpCodePoint(cp)) {
                            seq.append((char)cp);
                        } else {
                            seq.append(Character.highSurrogate(cp));
                            seq.append(Character.lowSurrogate(cp));
                        }
                    });
            textures.put(seq.toString(), entry);
        }
    }

    public @Nullable InputStream getTextureAsStream(String seq) throws IOException {
        final var entry = textures.get(seq);
        if (entry == null) return null;
        return getInputStream(entry);
    }
}
