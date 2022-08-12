package io.github.colintimbarndt.chat_emotes.common.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.common.data.unicode.pattern.MatchResult
import java.text.ParseException
import java.util.*
import java.util.regex.Pattern

data class JoinerSlot(
    val transformer: MatchTransformer,
    val count: Int,
    val sameMatcher: Boolean,
    val delimiter: String
) : MatchTransformer {
    override fun transform(r: MatchResult, result: StringBuilder) {
        transformer.transform(r, result)
    }

    override fun acceptsWidth(w: Int): Boolean {
        return transformer.acceptsWidth(w)
    }

    override val acceptedWidth: Int
        get() = transformer.acceptedWidth

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other.javaClass != this.javaClass) return false
        val that = other as JoinerSlot
        return transformer == that.transformer
                && count == that.count
                && sameMatcher == that.sameMatcher
                && delimiter == that.delimiter
    }

    override fun hashCode(): Int {
        return Objects.hash(transformer, count, sameMatcher, delimiter)
    }

    override fun toString(): String {
        return "JoinerSlot[" +
                "transformer=" + transformer + ", " +
                "count=" + count + ", " +
                "sameMatcher=" + sameMatcher + ", " +
                "delimiter=" + delimiter + ']'
    }

    companion object {
        /**
         * GRAMMAR:
         * <pre>
         * transformer : body? parameters?;
         * body        : matcher ("|" matcher)*
         * matcher     : CHAR "-" CHAR | NAME
         * parameters  : "#" quantifier ("&" delimiter)?
         * quantifier  : (NUMBER | "*" | "?")
         * delimiter   : [^]*
         * NAME        : CHAR+
         * CHAR        : [^#?|-]+
         * NUMBER      : [0-9]+
        </pre> *
         */
        private val SLOT_EXPR = Pattern.compile(
            "^(?<body>([^#?|-]-[^#?|-]|[^#?|-]+)(\\|([^#?|-]-[^#?|-]|[^#?|-]+))*)?" +
                    "(#(?<count>\\d+|\\*|\\?)(&(?<join>.*))?)?$"
        )

        @Throws(ParseException::class)
        fun parse(content: String, start: Int): JoinerSlot {
            val parts = SLOT_EXPR.matcher(content)
            if (!parts.matches()) {
                throw ParseException("Invalid Slot: $content", start)
            }
            var sameCapture = false
            val countStr = parts.group("count")
            val count: Int
            if (countStr == null) {
                count = 1
            } else {
                count = when (countStr) {
                    "*" -> Int.MAX_VALUE
                    "?" -> {
                        sameCapture = true
                        Int.MAX_VALUE
                    }
                    else -> countStr.toInt()
                }
            }
            var joinWith = parts.group("join")
            if (joinWith == null) joinWith = ""
            val body = parts.group("body")
            val transformer: MatchTransformer
            if (body == null) {
                transformer = IdentityTransformer
            } else {
                val values = body.split('|')
                val transformers = arrayOfNulls<MatchTransformer>(values.size)
                for ((i, v) in values.withIndex()) {
                    val minus = v.indexOf('-')
                    if (minus >= 0) {
                        transformers[i] = RangeTransformer(v.codePointAt(0), v.codePointAt(minus + 1))
                    } else {
                        transformers[i] = ConstantTransformer(v)
                    }
                }
                transformer = EnumeratedTransformer(transformers.requireNoNulls())
            }
            return JoinerSlot(transformer, count, sameCapture, joinWith)
        }
    }
}