package io.github.colintimbarndt.chat_emotes.data.unicode

import io.github.colintimbarndt.chat_emotes.util.StringBuilderExt.plusAssign

/**
 * Zero width no join code points are used to display two code points as seperate characters that would otherwise
 * default to beint joined.
 *
 * One example are the skin tone modifier code points which are automatically joined with
 * emojis that can have a skin tone. (🧑🏽 becomes 🧑‌🏽)
 * @see [Unicode Table](https://unicode-table.com/en/200C/)
 */
const val ZWNJ = '\u200C'

/**
 * Zero width joiners are used to combine multiple code points into one character.
 *
 * One example are family emojis which are the emojis the family members joined by ZWJ (👨‍👩‍👦 is 👨‍‌👩‌‍👦).
 * @see [Unicode Table](https://unicode-table.com/en/200D/)
 */
const val ZWJ = '\u200D'

const val VS15 = '\uFE0E'

/**
 * An invisible codepoint which specifies that the preceding character should be displayed as an emoji.
 * Only required if the preceding character defaults to text presentation.
 * @see [Unicode Table](https://unicode-table.com/en/FE0F/)
 */
const val VS16 = '\uFE0F'

/**
 * Ends a tag sequence
 * @see [Unicode Table](https://unicode-table.com/en/E007F/)
 */
const val CANCEL = "\udb40\udc7f"

/**
 * Creates a tag sequence. Tag sequences are typically used as modifiers for characters where no modifier code point
 * exists. One example is the flag of england (🏴󠁧󠁢󠁥󠁮󠁧󠁿), which is a black flag combined with the tag sequence `gbeng`.
 * @param s content of the tag
 * @return unicode tag sequence
 */
fun tagged(s: String): String {
    val builder = StringBuilder(2 * s.length + 2)
    s.codePoints().forEachOrdered {
        require(it < 0x7F) { "Illegal characters in string" }
        builder.appendCodePoint(it + 0xE0000)
    }
    builder += CANCEL
    return builder.toString()
}