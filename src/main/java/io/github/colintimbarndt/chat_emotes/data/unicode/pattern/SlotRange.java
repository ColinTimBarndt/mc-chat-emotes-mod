package io.github.colintimbarndt.chat_emotes.data.unicode.pattern;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;

public record SlotRange(int min, int max) {

    public static final SlotRange ONE = new SlotRange(1, 1);
    public static final SlotRange GREEDY = new SlotRange(0, Integer.MAX_VALUE);

    public static @NotNull SlotRange parse(String minStr, String maxStr, String source, int start) throws ParseException {
        if (minStr == null) return ONE;
        if (minStr.equals("*")) return GREEDY;

        int min, max;
        min = Integer.parseUnsignedInt(minStr);
        if (maxStr != null) {
            max = Integer.parseUnsignedInt(maxStr);
            if (max < min) {
                throw new ParseException("Slot maximum is less than the minimum: " + source, start);
            }
        } else {
            max = min;
        }
        return new SlotRange(min, max);
    }
}
