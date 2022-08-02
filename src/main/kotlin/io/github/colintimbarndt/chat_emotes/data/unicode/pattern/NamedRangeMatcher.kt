package io.github.colintimbarndt.chat_emotes.data.unicode.pattern

data class NamedRangeMatcher(val fromName: String, val toName: String) : NamedMatcher<RangeMatcher> {
    override fun getNameReferences() = sequence {
        yield(fromName)
        yield(toName)
    }

    override fun resolveNames(resolve: (String) -> String): RangeMatcher {
        val from = toCodePoint(resolve(fromName))
        val to = toCodePoint(resolve(toName))
        return if (from <= to) RangeMatcher(from, to) else RangeMatcher(to, from, true)
    }

    companion object {
        private fun toCodePoint(s: String): Int {
            return if (s.isNotEmpty()) s.codePointAt(0) else 0
        }
    }
}