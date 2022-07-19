package io.github.colintimbarndt.chat_emotes.data;

import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult;
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnicodePatternTest {

    private static final HashMap<String, String> EMOTES = new HashMap<>() {{
        put("flag_white", Character.toString(0x1F3F3));
        put("flag_black", Character.toString(0x1F3F4));
        put("rainbow", Character.toString(0x1f308));
        put("boy", Character.toString(0x1f466));
        put("girl", Character.toString(0x1f467));
        put("man", Character.toString(0x1f468));
        put("woman", Character.toString(0x1f469));
        put("regional_indicator_a", Character.toString(0x1F1E6));
        put("regional_indicator_z", Character.toString(0x1F1FF));
    }};
    private static final String[] VALID_INPUTS = {
            "{flag_black}T(gbeng)",
            "{flag_white}E+{rainbow}",
            "{man|woman#1-2&+}+{boy|girl#1-2&+}",
            "{regional_indicator_a-regional_indicator_z#2}"
    };
    private static final String[][] VALID_MATCHES = {
            // 🏴󠁧󠁢󠁥󠁮󠁧󠁿
            {"\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F"},
            // 🏳️‍🌈
            {"\uD83C\uDFF3️\u200D\uD83C\uDF08"},
            {
                    // 👨‍👩‍👧‍👦
                    "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66",
                    // 👨‍👩‍👧
                    "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67",
                    // 👨‍👧
                    "\uD83D\uDC68\u200D\uD83D\uDC67",
                    // 👩‍👧‍👦
                    "\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66",
            },
            // 🇨🇦
            {"\uD83C\uDDE8\uD83C\uDDE6"},
    };
    private static final MatchResult[][][][] EXPECTED_CAPTURES = {
            {
                    // 🏴󠁧󠁢󠁥󠁮󠁧󠁿
                    {
                            // 🏴
                            {new MatchResult("\ud83c\udff4", 0, "flag_black")}
                    }
            },
            {
                    // 🏳️‍🌈
                    {
                            // 🏳
                            {new MatchResult("\ud83c\udff3", 0, "flag_white")},
                            // 🌈
                            {new MatchResult("\uD83C\uDF08", 0, "rainbow")},
                    }
            },
            {
                    // 👨‍👩‍👧‍👦
                    {
                            // 👨, 👩
                            {new MatchResult("\uD83D\uDC68", 0, "man"), new MatchResult("\uD83D\uDC69", 1, "woman")},
                            // ‍👧, 👦
                            {new MatchResult("\uD83D\uDC67", 1, "girl"), new MatchResult("\uD83D\uDC66", 0, "boy")},
                    },
                    // 👨‍👩‍👧
                    {
                            // 👨, 👩
                            {new MatchResult("\uD83D\uDC68", 0, "man"), new MatchResult("\uD83D\uDC69", 1, "woman")},
                            // ‍👧
                            {new MatchResult("\uD83D\uDC67", 1, "girl")},
                    },
                    // 👨‍👧
                    {
                            // 👨
                            {new MatchResult("\uD83D\uDC68", 0, "man")},
                            // ‍👧
                            {new MatchResult("\uD83D\uDC67", 1, "girl")},
                    },
                    // 👩‍👧‍👦
                    {
                            // 👩
                            {new MatchResult("\uD83D\uDC69", 1, "woman")},
                            // ‍👧, 👦
                            {new MatchResult("\uD83D\uDC67", 1, "girl"), new MatchResult("\uD83D\uDC66", 0, "boy")},
                    },
            },
            {
                    // 🇨🇦
                    {
                            // 🇨, 🇦
                            {new MatchResult("\uD83C\uDDE8", 2), new MatchResult("\uD83C\uDDE6", 0)},
                    }
            },
    };

    @Test
    void parse() {
        for(int i = 0; i < VALID_INPUTS.length; i++) {
            final var input = VALID_INPUTS[i];
            final var pattern = assertDoesNotThrow(() ->
                    UnicodePattern.parse(input)
            );
            pattern.resolveNames(EMOTES::get);
            for (int j = 0; j < VALID_MATCHES.length; j++) {
                for(int m = 0; m < VALID_MATCHES[j].length; m++) {
                    final var validMatch = VALID_MATCHES[j][m];
                    final var captures = pattern.apply(validMatch);
                    if (j == i) {
                        assertNotNull(captures, "testing " + validMatch);
                        final var expect = EXPECTED_CAPTURES[i][m];
                        for (int k = 0; k < captures.length; k++) {
                            assertEquals(expect[k].length, captures[k].size());
                            for (int l = 0; l < captures[k].size(); l++) {
                                assertEquals(expect[k][l], captures[k].get(l));
                            }
                        }
                    } else
                        assertNull(captures);
                }
            }
        }
    }

    @Test
    void parseNoCapture() {
        final var pattern = assertDoesNotThrow(() ->
                UnicodePattern.parse("{!flag_black}{flag_white}{!flag_black}{man}")
        );
        pattern.resolveNames(EMOTES::get);
        final var matches = pattern.apply(
                // 🏴🏳🏴👨
                "\uD83C\uDFF4\uD83C\uDFF3\uD83C\uDFF4\uD83D\uDC68"
        );
        assertNotNull(matches);
        assertArrayEquals(new List[] {
                List.of(new MatchResult("\uD83C\uDFF3", 0, "flag_white")),
                List.of(new MatchResult("\uD83D\uDC68", 0, "man")),
        }, matches);
    }

    @Test
    void parseEscaped() {
        final var pattern = assertDoesNotThrow(() ->
                UnicodePattern.parse("L{!don't }L{panic\\!}")
        );
        pattern.resolveNames(EMOTES::get);
        final var matches = pattern.apply(
                "don't panic!"
        );
        assertNotNull(matches);
        assertArrayEquals(new List[] {
                List.of(new MatchResult("panic!", 0, "panic!")),
        }, matches);
    }
}