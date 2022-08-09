package io.github.colintimbarndt.chat_emotes.data.unicode.pattern

data class NamedExactMatcher(val name: String) : NamedMatcher<ExactMatcher> {
    override fun getNameReferences() = sequence {
        yield(name)
    }

    override fun resolveNames(resolve: (String) -> String): ExactMatcher {
        return ExactMatcher(resolve(name), name)
    }
}