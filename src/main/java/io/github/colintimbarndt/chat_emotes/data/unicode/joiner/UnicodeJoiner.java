package io.github.colintimbarndt.chat_emotes.data.unicode.joiner;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern;
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.PatternSlot;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class UnicodeJoiner {
    private final ArrayList<JoinerSlot> slots = new ArrayList<>();
    private final ArrayList<String> literals = new ArrayList<>();

    private UnicodeJoiner() {}

    public static @NotNull UnicodeJoiner parse(@NotNull String s) throws ParseException {
        int i = 0;
        char c;
        final int sz = s.length();
        final var expr = new UnicodeJoiner();
        int currentLiteral = 0;
        for(; i < sz; i++) {
            c = s.charAt(i);
            if (c == '{') {
                if (i == 0) expr.literals.add("");
                else expr.literals.add(s.substring(currentLiteral, i));
                final int start = i;
                i = s.indexOf('}', start);
                if (i == -1) throw new ParseException("Unclosed Slot: " + s, start);
                final String content = s.substring(start + 1, i);
                expr.slots.add(JoinerSlot.parse(content, start));
                currentLiteral = i + 1;
            }
        }
        expr.literals.add(s.substring(currentLiteral, i));
        return expr;
    }

    public boolean isCompatibleWith(UnicodePattern pattern) {
        final var mss = pattern.getSlots().iterator();
        PatternSlot ms = null;
        int m = 0;
        for(var ts : slots) {
            int c = ts.count();
            if (m == 0) {
                if(!mss.hasNext()) return false;
                ms = mss.next();
                m = ms.range.min();
                if (!ts.acceptsWidth(ms.width())) return false;
            }
            if (ts.sameMatcher()) {
                if (ts.requiresName() && !ms.hasName()) return false;
                if (c == Integer.MAX_VALUE) {
                    // #& consumes the whole captures
                    m = 0;
                    continue;
                }
                if (ms.range.min() != c && ms.range.max() != c)
                    return false; // #X must consume whole captures
            } else {
                if (c == Integer.MAX_VALUE) {
                    // #* consumes all remaining captures
                    final boolean reqName = ts.requiresName();
                    while (mss.hasNext()) {
                        ms = mss.next();
                        if (!ts.acceptsWidth(ms.width())
                                || (reqName && !ms.hasName()))
                            return false;
                    }
                    return true;
                }
                if (ms.range.min() != ms.range.max())
                    return false; // Match must capture a consistent amount
                if (ts.requiresName() && !ms.hasName())
                    return false;
                m -= ts.count();
                while (m < 0) {
                    if (!mss.hasNext()) return false;
                    ms = mss.next();
                    if (ms.range.min() != ms.range.max())
                        return false;
                    if (ts.requiresName() && !ms.hasName())
                        return false;
                    m += ms.range.min();
                }
            }
        }
        return !mss.hasNext();
    }

    public String evaluate(final @NotNull List<MatchResult> @NotNull[] captures) {
        final StringBuilder builder = new StringBuilder();
        int sz = slots.size();
        int i = 0; // #slot
        int c = 0; // #capture
        int ci = 0; // #capture index
        builder.append(literals.get(i));
        if (captures.length == 0) return builder.toString();
        var capture = captures[c++];
        Outer: while (i < sz) {
            var slot = slots.get(i);
            boolean joiner = false;
            int consumed = 0;
            while (consumed < slot.count()) {
                if (ci == capture.size()) {
                    if (c == captures.length) break Outer;
                    ci = 0;
                    capture = captures[c++];
                    if (slot.sameMatcher()) break;
                }
                if (joiner) builder.append(slot.delimiter());
                joiner = true;
                slot.transform(capture.get(ci), builder);
                consumed++;
                ci++;
            }
            i++;
            builder.append(literals.get(i));
        }
        return builder.toString();
    }
}
