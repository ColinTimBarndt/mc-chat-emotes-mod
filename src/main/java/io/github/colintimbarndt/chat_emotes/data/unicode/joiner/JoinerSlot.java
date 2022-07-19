package io.github.colintimbarndt.chat_emotes.data.unicode.joiner;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.regex.Pattern;

public record JoinerSlot(
        MatchTransformer transformer,
        int count,
        boolean sameMatcher,
        String delimiter
) implements MatchTransformer {
    /**
     * GRAMMAR:
     * <pre>
     *     transformer : body? parameters?;
     *     body        : matcher ("|" matcher)*
     *     matcher     : CHAR "-" CHAR | NAME
     *     parameters  : "#" quantifier ("&" delimiter)?
     *     quantifier  : (NUMBER | "*" | "?")
     *     delimiter   : [^]*
     *     NAME        : CHAR+
     *     CHAR        : [^#?|-]+
     *     NUMBER      : [0-9]+
     * </pre>
     */
    private static final Pattern SLOT_EXPR = Pattern.compile(
            "^(?<body>([^#?|-]-[^#?|-]|[^#?|-]+)(\\|([^#?|-]-[^#?|-]|[^#?|-]+))*)?" +
                    "(#(?<count>\\d+|\\*|\\?)(&(?<join>.*))?)?$"
    );

    @Override
    public void transform(@NotNull MatchResult r, @NotNull StringBuilder result) {
        transformer.transform(r, result);
    }

    @Override
    public boolean acceptsWidth(int w) {
        return transformer.acceptsWidth(w);
    }

    @Override
    public int acceptedWidth() {
        return transformer.acceptedWidth();
    }

    public static @NotNull JoinerSlot parse(String content, int start) throws ParseException {
        final var parts = SLOT_EXPR.matcher(content);
        if (!parts.matches()) {
            throw new ParseException("Invalid Slot: " + content, start);
        }
        boolean sameCapture = false;
        final var countStr = parts.group("count");
        final int count;
        if (countStr == null) {
            count = 1;
        } else {
            count = switch (countStr) {
                default -> Integer.parseInt(countStr);
                case "*" -> Integer.MAX_VALUE;
                case "?" -> {
                    sameCapture = true;
                    yield Integer.MAX_VALUE;
                }
            };
        }
        var joinWith = parts.group("join");
        if (joinWith == null) joinWith = "";
        final var body = parts.group("body");
        final MatchTransformer transformer;
        if (body == null) {
            transformer = IdentityTransformer.INSTANCE;
        } else {
            final var values = body.split("\\|");
            final var transformers = new MatchTransformer[values.length];
            for(int i = 0; i < values.length; i++) {
                final var v = values[i];
                final var minus = v.indexOf('-');
                if (minus >= 0) {
                    transformers[i] = new RangeTransformer(v.codePointAt(0), v.codePointAt(minus + 1));
                } else {
                    transformers[i] = new ConstantTransformer(v);
                }
            }
            transformer = new EnumeratedTransformer(transformers);
        }
        return new JoinerSlot(transformer, count, sameCapture, joinWith);
    }
}
