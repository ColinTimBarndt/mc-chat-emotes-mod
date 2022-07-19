package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.github.colintimbarndt.chat_emotes.data.unicode.UnicodeUtil.*;

public final class UnicodePattern implements Function<String, List<MatchResult>[]> {
    private ArrayList<NamedPatternSlot> namedSlots = new ArrayList<>();
    private PatternSlot[] slots = null;
    private final ArrayList<String> literals = new ArrayList<>();
    private int numCaptured = 0;

    private UnicodePattern() {}

    public static @NotNull UnicodePattern parse(@NotNull String s) throws ParseException {
        int i = 0;
        char c;
        final int sz = s.length();
        final var expr = new UnicodePattern();
        StringBuilder currentLiteral = new StringBuilder();
        for(; i < sz; i++) {
            c = s.charAt(i);
            switch (c) {
                case '{', 'L', 'U' -> {
                    if (currentLiteral.isEmpty()) {
                        expr.literals.add("");
                    } else {
                        expr.literals.add(currentLiteral.toString());
                        currentLiteral = new StringBuilder();
                    }
                    var mode = NamedPatternSlot.Mode.Named;
                    if (c != '{') {
                        mode = switch (c) {
                            case 'L' -> NamedPatternSlot.Mode.Literal;
                            case 'U' -> NamedPatternSlot.Mode.Unicode;
                            default -> null;
                        };
                        if (++i == sz) {
                            throw new ParseException("Missing body of Slot: " + s, i);
                        }
                        c = s.charAt(i);
                        if (c != '{') {
                            throw new ParseException("Expected body of Slot: " + s, i);
                        }
                    }
                    final int start = i;
                    i = s.indexOf('}', start);
                    if (i == -1) throw new ParseException("Unclosed Slot: " + s, start);
                    final String content = s.substring(start+1, i);
                    final var slot = NamedPatternSlot.parse(content, start, mode);
                    if (slot.captured) expr.numCaptured++;
                    expr.namedSlots.add(slot);
                }
                case 'T' -> {
                    if (++i == sz) {
                        throw new ParseException("Missing body of Tag: " + s, i);
                    }
                    c = s.charAt(i);
                    if (c != '(') {
                        throw new ParseException("Expected body of Tag: " + s, i);
                    }
                    final int start = i;
                    i = s.indexOf(')', start);
                    if (i == -1) throw new ParseException("Unclosed body of Tag: " + s, start);
                    final var tag = tagged(s.substring(start+1, i));
                    currentLiteral.append(tag);
                }
                case 'E' -> currentLiteral.append(VS16);
                case '+' -> currentLiteral.append(ZWJ);
                default -> throw new ParseException("Invalid character '%s': %s".formatted(c, s), i);
            }
        }
        expr.literals.add(currentLiteral.toString());
        return expr;
    }

    public void getNameReferences(Consumer<String> consumer) {
        if (this.namedSlots == null) return;
        for (var slot : namedSlots)
            slot.getNameReferences(consumer);
    }

    public void resolveNames(Function<String, String> resolver) {
        final var slots = new PatternSlot[namedSlots.size()];
        for (int i = 0; i < slots.length; i++) {
            final var ns = namedSlots.get(i);
            slots[i] = ns.resolveNames(resolver);
        }
        this.slots = slots;
        this.namedSlots = null;
    }

    public Stream<PatternSlot> getSlots() {
        return Arrays.stream(slots).filter(s -> s.captured);
    }

    @Override
    public @Nullable List<@NotNull MatchResult>[] apply(@NotNull String emote) {
        if (slots == null) throw new IllegalStateException("names not resolved");
        var lit = literals.get(0);
        if (!emote.startsWith(lit)) return null;
        @SuppressWarnings("unchecked")
        final var result = (List<MatchResult>[]) new List[numCaptured];
        int i = lit.length();
        int ri = 0;
        final int sz = emote.length();
        for (int k = 0; k < slots.length;) {
            final PatternSlot slot = slots[k];
            if (i == sz) return null;
            int nc = 0;
            var captures = slot.captured ? new ArrayList<MatchResult>(slot.range.min()) : null;
            int i2 = i; // lookahead for the delimiter
            while (true) {
                final var mRes = slot.matchWith(emote, i2);
                if (mRes == null) break; // abort match
                if (captures != null) captures.add(mRes);
                nc++;
                i = i2 + mRes.value().length(); // no backtracking needed anymore
                if (nc == slot.range.max()) break;
                if (!emote.startsWith(slot.delimiter, i)) break;
                i2 = i + slot.delimiter.length();
            }
            if (nc < slot.range.min()) return null;
            if (captures != null)
                result[ri++] = captures;
            k++;
            lit = literals.get(k);
            if (!emote.startsWith(lit, i)) return null;
            i += lit.length();
        }
        return result;
    }

}
