@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package io.github.colintimbarndt.chat_emotes_util

import io.karma.sliced.Slice

inline operator fun <T, S> S.get(start: Int, end: Int): S where S: Slice<T> = slice(start, end) as S

inline operator fun <T, S> S.get(range: IntRange): S where S: Slice<T> = slice(range.first, range.last + 1) as S