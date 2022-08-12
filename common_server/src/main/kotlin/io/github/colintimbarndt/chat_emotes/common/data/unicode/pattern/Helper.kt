package io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern

internal inline fun <S, M> S.equalsHelper(matchers: S.() -> Array<M>, other: S): Boolean
        where S: IPatternSlot {
    if (!matchers().contentEquals(other.matchers())) return false
    if (range != other.range) return false
    if (delimiter != other.delimiter) return false
    if (captured != other.captured) return false
    return true
}
