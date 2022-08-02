package io.github.colintimbarndt.chat_emotes.data.unicode.pattern

import io.github.colintimbarndt.chat_emotes.data.unicode.VS16
import io.github.colintimbarndt.chat_emotes.data.unicode.ZWJ
import io.github.colintimbarndt.chat_emotes.data.unicode.ZWNJ
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.SlotQuantifier.Companion.parse
import java.text.ParseException
import java.util.regex.Pattern

/**
 * Slots are just like capture groups in [Regular Expressions][Pattern] but with the limitation that they cannot be
 * nested. A named slot consists of [NamedMatcher]s that will be tested in series and the first match will be returned
 */
data class NamedPatternSlot(
    private val matchers: Array<NamedMatcher<*>>,
    override val range: SlotQuantifier,
    override val delimiter: String,
    override val captured: Boolean,
    val mode: Mode
) : IPatternSlot, NamedMatcher<PatternSlot> {

    override fun getNameReferences() = sequence {
        if (mode == Mode.Named) {
            for (m in matchers)
                yieldAll(m.getNameReferences())
        }
    }

    override fun resolveNames(resolve: (String) -> String): PatternSlot {
        val resolve0 = when (mode) {
            Mode.Named -> resolve
            Mode.Literal -> { x -> x }
            Mode.Unicode -> { code -> Character.toString(code.toInt(16)) }
        }
        val matchers = arrayOfNulls<Matcher>(matchers.size)
        for (i in this.matchers.indices) {
            val m = this.matchers[i].resolveNames(resolve0)
            matchers[i] = m
        }
        return PatternSlot(matchers.requireNoNulls(), range, delimiter, captured)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NamedPatternSlot

        if (mode != other.mode) return false
        return equalsHelper(NamedPatternSlot::matchers, other)
    }

    override fun hashCode(): Int {
        var result = matchers.contentHashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + delimiter.hashCode()
        result = 31 * result + captured.hashCode()
        result = 31 * result + mode.hashCode()
        return result
    }

    /**
     * Slot modes determine how names will be resolved and how the slot is parsed to ensure correct syntax
     */
    enum class Mode {
        /**
         * Names are resolved directly
         */
        Named,

        /**
         * Names are taken literally (resolver is the identity function)
         */
        Literal,

        /**
         * Names are parsed as hexadecimal unicode code points
         * @see UNICODE_SLOT_EXPR
         */
        Unicode
    }

    companion object {
        private const val EXPR_BODY_TEMPLATE = "^(?<body>!?B+(-B+)?(\\|B+(-B+)?)*)"
        private const val EXPR_PARAMETERS = "(#(?<min>\\d+|(\\*(?!-)))(-(?<max>\\d+))?(&(?<delim>.*))?)?$"

        /**
         * GRAMMAR:
         * ```g4
         * pattern    : body parameters?;
         * body       : matcher ("|" matcher)*
         * matcher    : NAME ("-" NAME)?
         * parameters : "#" quantifier ("&" DELIMITER)?
         * quantifier : (range | NUMBER | "*")
         * range      : NUMBER "-" NUMBER
         * DELIMITER  : [^]*
         * NAME       : ([^#&?!|\-] | "\\" [#&?!|\\-])+
         * NUMBER     : [0-9]+
         * ```
         */
        private val SLOT_EXPR = Pattern.compile(
            EXPR_BODY_TEMPLATE.replace("B", "([^#&?!|\\\\-]|\\\\[#&?!|\\\\-])") +
                    EXPR_PARAMETERS
        )

        /**
         * This represents a subset of [SLOT_EXPR] with the restriction on `NAME` being that it must be a hexadecimal
         * integer representing a unicode code point
         * ```g4
         * NAME : [0-9a-fA-F]
         * ```
         */
        private val UNICODE_SLOT_EXPR = Pattern.compile(
            EXPR_BODY_TEMPLATE.replace("B", "[\\da-f]") +
                    EXPR_PARAMETERS,
            Pattern.CASE_INSENSITIVE
        )

        @JvmStatic
        @Throws(ParseException::class)
        fun parse(content: String, start: Int, mode: Mode): NamedPatternSlot {
            val parts = when (mode) {
                Mode.Unicode -> UNICODE_SLOT_EXPR
                else -> SLOT_EXPR
            }.matcher(content)
            if (!parts.matches()) {
                throw ParseException("Invalid Slot: $content", start)
            }
            val minStr = parts.group("min")
            val maxStr = parts.group("max")
            val range = parse(minStr, maxStr, content, start)
            val joinWithStr = parts.group("delim")
            val joinWith: String = if (joinWithStr == null) {
                ""
            } else {
                val builder = StringBuilder(joinWithStr.length * 2)
                for (ch in joinWithStr) {
                    builder.append(
                        when (ch) {
                            '+' -> ZWJ
                            '!' -> ZWNJ
                            'E' -> VS16
                            else -> throw ParseException("Invalid character '$ch': $content", start)
                        }
                    )
                }
                builder.toString()
            }
            var body = parts.group("body")
            var captured = true
            if (body.startsWith('!')) {
                body = body.substring(1)
                captured = false
            }
            run {
                var i = body.indexOf('\\')
                if (i >= 0) {
                    val bodyBuilder = StringBuilder(body.length - 1)
                    var clipStart = 0
                    while (i >= 0) {
                        bodyBuilder.append(body, clipStart, i)
                        clipStart = i + 1
                        i += 2
                        i = body.indexOf('\\', i + 1)
                    }
                    bodyBuilder.append(body, clipStart, body.length)
                    body = bodyBuilder.toString()
                }
            }
            val matcherStrings = body.split('|')
            val matchers = arrayOfNulls<NamedMatcher<*>>(matcherStrings.size)
            for ((i, ms) in matcherStrings.withIndex()) {
                val minus = ms.indexOf('-')
                if (minus >= 0) {
                    matchers[i] = NamedRangeMatcher(ms.substring(0, minus), ms.substring(minus + 1))
                } else {
                    matchers[i] = NamedExactMatcher(ms)
                }
            }
            return NamedPatternSlot(matchers.requireNoNulls(), range, joinWith, captured, mode)
        }
    }
}