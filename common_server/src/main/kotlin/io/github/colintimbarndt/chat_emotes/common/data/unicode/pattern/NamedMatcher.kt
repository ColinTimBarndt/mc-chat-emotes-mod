package io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern

/**
 * This is the named variant of [Matcher], meaning that it possibly contains [named references][getNameReferences]
 * to values that need to be resolved first using [resolveNames] in order to use it as a proper [Matcher]
 */
interface NamedMatcher<T : Matcher> {
    fun getNameReferences(): Sequence<String>
    fun resolveNames(resolve: (String) -> String): T
}