package io.github.colintimbarndt.chat_emotes.data;

import io.github.colintimbarndt.chat_emotes.data.unicode.joiner.UnicodeJoiner;
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern;
import io.github.colintimbarndt.chat_emotes.util.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class UnicodeJoinerTest {
    private static UnicodePattern[] PATTERNS;
    private static final HashMap<String, String> EMOTES = new HashMap<>() {{
        put("boy", Character.toString(0x1f466));
        put("girl", Character.toString(0x1f467));
        put("man", Character.toString(0x1f468));
        put("woman", Character.toString(0x1f469));
        put("regional_indicator_a", Character.toString(0x1F1E6));
        put("regional_indicator_z", Character.toString(0x1F1FF));
    }};

    static {
        PATTERNS = new UnicodePattern[2];
        try {
            PATTERNS[0] = UnicodePattern.parse("{regional_indicator_a-regional_indicator_z#2}");
            PATTERNS[1] = UnicodePattern.parse("{man|woman#1-2&+}+{boy|girl#1-2&+}");
        } catch (ParseException ex) {
            PATTERNS = ArrayUtils.emptyArray();
        }
    }

    @Test
    void transformFlags() {
        final UnicodePattern pattern = assertDoesNotThrow(() ->
                UnicodePattern.parse("{regional_indicator_a-regional_indicator_z#2}")
        );
        pattern.resolveNames(EMOTES::get);
        final UnicodeJoiner joiner1 = assertDoesNotThrow(() ->
                UnicodeJoiner.parse("flag_{a-z#2}")
        );
        assertTrue(joiner1.isCompatibleWith(pattern), "joiner1 compatible");
        final UnicodeJoiner joiner2 = assertDoesNotThrow(() ->
                UnicodeJoiner.parse("flag_{a-z}{a-z}")
        );
        assertTrue(joiner2.isCompatibleWith(pattern), "joiner2 compatible");
        // 🇨🇦
        final var canada = "\uD83C\uDDE8\uD83C\uDDE6";
        final var captures = pattern.apply(canada);
        assertNotNull(captures);
        final var result1 = joiner1.evaluate(captures);
        assertEquals("flag_ca", result1);
        final var result2 = joiner2.evaluate(captures);
        assertEquals("flag_ca", result2);
    }

    @Test
    void transformFamily() {
        final UnicodePattern pattern = assertDoesNotThrow(() ->
                UnicodePattern.parse("{man|woman#1-2&+}+{boy|girl#1-2&+}")
        );
        pattern.resolveNames(EMOTES::get);
        final UnicodeJoiner joiner1 = assertDoesNotThrow(() ->
                UnicodeJoiner.parse("family_{m|w#?}{b|g#?}")
        );
        assertTrue(joiner1.isCompatibleWith(pattern), "joiner1 compatible");
        final UnicodeJoiner joiner2 = assertDoesNotThrow(() ->
                UnicodeJoiner.parse("family_{#*&_}")
        );
        assertTrue(joiner2.isCompatibleWith(pattern), "joiner2 compatible");
        // 👨‍👩‍👧‍👦
        final var family_mwgb = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";
        final var captures = pattern.apply(family_mwgb);
        assertNotNull(captures);
        final var result1 = joiner1.evaluate(captures);
        assertEquals("family_mwgb", result1);
        final var result2 = joiner2.evaluate(captures);
        assertEquals("family_man_woman_girl_boy", result2);
    }
}