package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.github.colintimbarndt.chat_emotes.data.unicode.UnicodeUtil.*;

public final class NamedPatternSlot implements NamedMatcher<PatternSlot> {
    private static final String EXPR_BODY_TEMPLATE = "^(?<body>!?B+(-B+)?(\\|B+(-B+)?)*)";
    private static final String EXPR_PARAMETERS = "(#(?<min>\\d+|(\\*(?!-)))(-(?<max>\\d+))?(&(?<delim>.*))?)?$";
    /**
     * GRAMMAR:
     * <pre>
     *     pattern    : body parameters?;
     *     body       : matcher ("|" matcher)*
     *     matcher    : NAME ("-" NAME)?
     *     parameters : "#" quantifier ("&" DELIMITER)?
     *     quantifier : (range | NUMBER | "*")
     *     range      : NUMBER "-" NUMBER
     *     DELIMITER  : [^]*
     *     NAME       : ([^#&?!|\-] | "\" [#&?!|\-])+
     *     NUMBER     : [0-9]+
     * </pre>
     */
    private static final Pattern SLOT_EXPR = Pattern.compile(
            EXPR_BODY_TEMPLATE.replace("B", "([^#&?!|\\\\-]|\\\\[#&?!|\\\\-])") +
                    EXPR_PARAMETERS
    );
    private static final Pattern UNICODE_SLOT_EXPR = Pattern.compile(
            EXPR_BODY_TEMPLATE.replace("B", "[\\da-z]") +
                    EXPR_PARAMETERS,
            Pattern.CASE_INSENSITIVE
    );

    private final @NotNull NamedMatcher<?> @NotNull[] matchers;
    public final @NotNull SlotRange range;
    public final @NotNull String delimiter;
    public final boolean captured;
    public final Mode mode;

    public NamedPatternSlot(
            @NotNull NamedMatcher<?> @NotNull[] matchers,
            @NotNull SlotRange range,
            @NotNull String delimiter,
            boolean captured,
            Mode mode
    ) {
        this.matchers = matchers;
        this.range = range;
        this.delimiter = delimiter;
        this.captured = captured;
        this.mode = mode;
    }

    @Override
    public void getNameReferences(Consumer<String> consumer) {
        if (mode != Mode.Named) return;
        for (var m : matchers)
            m.getNameReferences(consumer);
    }

    @Override
    public PatternSlot resolveNames(Function<String, String> resolver) {
        resolver = switch (mode) {
            case Named -> resolver;
            case Literal -> Function.identity();
            case Unicode -> code -> Character.toString(Integer.parseInt(code, 16));
        };
        final var matchers = new Matcher[this.matchers.length];
        for (int i = 0; i < this.matchers.length; i++) {
            final var m = this.matchers[i].resolveNames(resolver);
            matchers[i] = m;
        }
        return new PatternSlot(matchers, range, delimiter, captured);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull NamedPatternSlot parse(String content, int start, Mode mode) throws ParseException {
        final var parts = (switch (mode) {
            case Named, Literal -> SLOT_EXPR;
            case Unicode -> UNICODE_SLOT_EXPR;
        }).matcher(content);
        if (!parts.matches()) {
            throw new ParseException("Invalid Slot: " + content, start);
        }
        var minStr = parts.group("min");
        var maxStr = parts.group("max");
        var range = SlotRange.parse(minStr, maxStr, content, start);
        var joinWithStr = parts.group("delim");
        String joinWith;
        if (joinWithStr == null) {
            joinWith = "";
        } else {
            var builder = new StringBuilder(joinWithStr.length() * 2);
            for (int i = 0; i < joinWithStr.length(); i++) {
                final char ch = joinWithStr.charAt(i);
                builder.append(switch (ch) {
                    case '+' -> ZWJ;
                    case '!' -> ZWNJ;
                    case 'E' -> VS16;
                    default -> throw new ParseException("Invalid character '%s': %s".formatted(ch, content), start);
                });
            }
            joinWith = builder.toString();
        }
        var body = parts.group("body");
        boolean captured = true;
        if (body.startsWith("!")) {
            body = body.substring(1);
            captured = false;
        }
        {
            int i = body.indexOf('\\');
            if (i >= 0) {
                final var bodyBuilder = new StringBuilder(body.length() - 1);
                int clipStart = 0;
                for (; i >= 0; i = body.indexOf('\\', i + 1)) {
                    bodyBuilder.append(body, clipStart, i);
                    clipStart = i + 1;
                    i += 2;
                }
                bodyBuilder.append(body, clipStart, body.length());
                body = bodyBuilder.toString();
            }
        }
        final String[] matcherStrings = body.split("\\|");
        final NamedMatcher<?>[] matchers = new NamedMatcher[matcherStrings.length];
        for (int i = 0; i < matchers.length; i++) {
            final var ms = matcherStrings[i];
            final var minus = ms.indexOf('-');
            if (minus >= 0) {
                matchers[i] = new NamedRangeMatcher(ms.substring(0, minus), ms.substring(minus + 1));
            } else {
                matchers[i] = new NamedExactMatcher(ms);
            }
        }
        return new NamedPatternSlot(matchers, range, joinWith, captured, mode);
    }

    public enum Mode {
        Named,
        Literal,
        Unicode
    }
}
