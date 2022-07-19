package io.github.colintimbarndt.chat_emotes.util;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class BomAwareReader extends InputStreamReader {
    private static final ByteOrderMark[] BO_MARKS = {
            ByteOrderMark.UTF_16LE,
            ByteOrderMark.UTF_16BE,
            ByteOrderMark.UTF_8,
    };

    private BomAwareReader(InputStream delegate, Charset charset) {
        super(delegate, charset);
    }

    public static BomAwareReader create(InputStream delegate) throws IOException {
        final var bomStream = new BOMInputStream(delegate, BO_MARKS);
        final Charset charset;
        if (bomStream.hasBOM()) {
            charset = switch (bomStream.getBOMCharsetName()) {
                case "UTF-16LE" -> StandardCharsets.UTF_16LE;
                case "UTF-16BE" -> StandardCharsets.UTF_16BE;
                default -> StandardCharsets.UTF_8;
            };
        } else {
            charset = StandardCharsets.UTF_8;
        }
        return new BomAwareReader(bomStream, charset);
    }

    public static BufferedReader createBuffered(InputStream delegate) throws IOException {
        return new BufferedReader(create(delegate));
    }

    public static BufferedReader createBuffered(InputStream delegate, int sz) throws IOException {
        return new BufferedReader(create(delegate), sz);
    }
}
