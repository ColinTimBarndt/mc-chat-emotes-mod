package io.github.colintimbarndt.chat_emotes_util

val LTR_CATEGORIES = sortedSetOf(
    CharDirectionality.LEFT_TO_RIGHT,
    CharDirectionality.BOUNDARY_NEUTRAL,
    CharDirectionality.OTHER_NEUTRALS,
    CharDirectionality.UNDEFINED,
)

val Char.isLTR inline get() = directionality in LTR_CATEGORIES