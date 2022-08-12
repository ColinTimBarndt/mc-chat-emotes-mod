package io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern

internal interface IPatternSlot {
    val range: SlotQuantifier
    val delimiter: String
    val captured: Boolean
}
