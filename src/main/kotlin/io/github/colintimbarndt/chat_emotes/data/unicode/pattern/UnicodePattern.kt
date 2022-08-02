package io.github.colintimbarndt.chat_emotes.data.unicode.pattern

import io.github.colintimbarndt.chat_emotes.data.unicode.VS16
import io.github.colintimbarndt.chat_emotes.data.unicode.ZWJ
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.NamedPatternSlot.Companion.parse
import io.github.colintimbarndt.chat_emotes.data.unicode.tagged
import java.text.ParseException
import java.util.*
import java.util.stream.Stream

class UnicodePattern private constructor() {
    private var namedSlots: ArrayList<NamedPatternSlot>? = ArrayList()
    private var patternSlots: Array<PatternSlot>? = null
    private val literals = ArrayList<String>()
    private var numCaptured = 0

    fun getNameReferences() = sequence {
        if (namedSlots != null) {
            for (slot in namedSlots!!)
                yieldAll(slot.getNameReferences())
        }
    }

    fun resolveNames(resolver: (String) -> String) {
        val slots = arrayOfNulls<PatternSlot>(namedSlots!!.size)
        for (i in slots.indices) {
            val ns = namedSlots!![i]
            slots[i] = ns.resolveNames(resolver)
        }
        this.patternSlots = slots.requireNoNulls()
        namedSlots = null
    }

    fun getCapturedSlots(): Stream<PatternSlot> =
        Arrays.stream(patternSlots).filter { s: PatternSlot -> s.captured }

    @Suppress("NOTHING_TO_INLINE")
    inline fun apply(emote: String) = this(emote)

    operator fun invoke(emote: String): Array<List<MatchResult>>? {
        checkNotNull(patternSlots) { "names not resolved" }
        var lit = literals[0]
        if (!emote.startsWith(lit)) return null
        val result = arrayOfNulls<List<MatchResult>?>(numCaptured)
        var i = lit.length
        var ri = 0
        val sz = emote.length
        var k = 0
        while (k < patternSlots!!.size) {
            val slot = patternSlots!![k]
            if (i == sz) return null
            var nc = 0
            val captures = if (slot.captured) ArrayList<MatchResult>(
                slot.range.min
            ) else null
            var i2 = i // lookahead for the delimiter
            while (true) {
                val mRes = slot.matchWith(emote, i2) ?: break
                // abort match
                captures?.add(mRes)
                nc++
                i = i2 + mRes.value.length // no backtracking needed anymore
                if (nc == slot.range.max) break
                if (!emote.startsWith(slot.delimiter, i)) break
                i2 = i + slot.delimiter.length
            }
            if (nc < slot.range.min) return null
            if (captures != null) result[ri++] = captures
            k++
            lit = literals[k]
            if (!emote.startsWith(lit, i)) return null
            i += lit.length
        }
        return result.requireNoNulls()
    }

    companion object {
        @JvmStatic
        @Throws(ParseException::class)
        fun parse(s: String): UnicodePattern {
            var i = 0
            var c: Char
            val sz = s.length
            val expr = UnicodePattern()
            var currentLiteral = StringBuilder()
            while (i < sz) {
                c = s[i]
                when (c) {
                    '{', 'L', 'U' -> {
                        if (currentLiteral.isEmpty()) {
                            expr.literals.add("")
                        } else {
                            expr.literals.add(currentLiteral.toString())
                            currentLiteral = StringBuilder()
                        }
                        var mode: NamedPatternSlot.Mode? = NamedPatternSlot.Mode.Named
                        if (c != '{') {
                            mode = when (c) {
                                'L' -> NamedPatternSlot.Mode.Literal
                                'U' -> NamedPatternSlot.Mode.Unicode
                                else -> null
                            }
                            if (++i == sz) {
                                throw ParseException("Missing body of Slot: $s", i)
                            }
                            c = s[i]
                            if (c != '{') {
                                throw ParseException("Expected body of Slot: $s", i)
                            }
                        }
                        val start = i
                        i = s.indexOf('}', start)
                        if (i == -1) throw ParseException("Unclosed Slot: $s", start)
                        val content = s.substring(start + 1, i)
                        val slot = parse(content, start, mode!!)
                        if (slot.captured) expr.numCaptured++
                        expr.namedSlots!!.add(slot)
                    }

                    'T' -> {
                        if (++i == sz) {
                            throw ParseException("Missing body of Tag: $s", i)
                        }
                        c = s[i]
                        if (c != '(') {
                            throw ParseException("Expected body of Tag: $s", i)
                        }
                        val start = i
                        i = s.indexOf(')', start)
                        if (i == -1) throw ParseException("Unclosed body of Tag: $s", start)
                        val tag = tagged(s.substring(start + 1, i))
                        currentLiteral.append(tag)
                    }

                    'E' -> currentLiteral.append(VS16)
                    '+' -> currentLiteral.append(ZWJ)
                    else -> throw ParseException("Invalid character '$c': $s", i)
                }
                i++
            }
            expr.literals.add(currentLiteral.toString())
            return expr
        }
    }
}