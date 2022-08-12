package io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern

import java.text.ParseException

/**
 * Stores how many times a slot can be repeated in the expression
 */
data class SlotQuantifier(val min: Int, val max: Int) {
    companion object {
        private val ONE: SlotQuantifier = SlotQuantifier(1, 1)
        private val GREEDY: SlotQuantifier = SlotQuantifier(0, Int.MAX_VALUE)

        @Throws(ParseException::class)
        fun parse(minStr: String?, maxStr: String?, source: String, start: Int): SlotQuantifier {
            if (minStr == null) {
                assert(maxStr == null)
                return ONE
            }
            if (minStr == "*") {
                assert(maxStr == null)
                return GREEDY
            }
            val max: Int
            val min: Int = Integer.parseUnsignedInt(minStr)
            if (maxStr != null) {
                max = Integer.parseUnsignedInt(maxStr)
                if (max < min) {
                    throw ParseException("Slot maximum is less than the minimum: $source", start)
                }
            } else {
                max = min
            }
            return SlotQuantifier(min, max)
        }
    }
}