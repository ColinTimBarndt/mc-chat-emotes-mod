package io.github.colintimbarndt.chat_emotes.data.unicode.joiner

import io.github.colintimbarndt.chat_emotes.data.unicode.joiner.JoinerSlot.Companion.parse
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.MatchResult
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.PatternSlot
import io.github.colintimbarndt.chat_emotes.data.unicode.pattern.UnicodePattern
import java.text.ParseException

class UnicodeJoiner private constructor() {
    private val slots = ArrayList<JoinerSlot>()
    private val literals = ArrayList<String>()
    fun isCompatibleWith(pattern: UnicodePattern): Boolean {
        val patternSlots: Iterator<PatternSlot> = pattern.getCapturedSlots().iterator()
        var patternSlot: PatternSlot? = null
        var minRem = 0
        var maxOffset = 0
        if (patternSlots.hasNext()) {
            patternSlot = patternSlots.next()
            minRem = patternSlot.range.min
            maxOffset = patternSlot.range.max - minRem
        } else {
            if (slots.isNotEmpty()) return false
        }
        for (ts in slots) {
            val c = ts.count
            if (minRem == 0) {
                if (maxOffset != 0) return false
                if (!patternSlots.hasNext()) return false
                patternSlot = patternSlots.next()
                minRem = patternSlot.range.min
                maxOffset = patternSlot.range.max - minRem
            }
            patternSlot!!
            if (!ts.acceptsWidth(patternSlot.width)) return false
            if (ts.sameMatcher) {
                if (ts.requiresName && !patternSlot.hasName) return false
                if (c == Int.MAX_VALUE) {
                    // #& consumes the whole current captures
                    minRem = 0
                    maxOffset = 0
                    continue
                }
                if (minRem < c) return false
                minRem -= c
            } else {
                // whole-matcher
                if (c == Int.MAX_VALUE) {
                    // #* consumes all remaining captures
                    val reqName = ts.requiresName
                    while (patternSlots.hasNext()) {
                        patternSlot = patternSlots.next()
                        if (!ts.acceptsWidth(patternSlot.width)) return false
                        if (reqName && !patternSlot.hasName) return false
                    }
                    return true
                }
                if (maxOffset != 0) return false // Match must capture a consistent amount
                if (ts.requiresName && !patternSlot.hasName) return false
                minRem -= ts.count
                while (minRem < 0) {
                    if (!patternSlots.hasNext()) return false
                    patternSlot = patternSlots.next()
                    if (!ts.acceptsWidth(patternSlot.width)) return false
                    if (patternSlot.range.min != patternSlot.range.max) return false
                    if (ts.requiresName && !patternSlot.hasName) return false
                    minRem += patternSlot.range.min
                }
            }
        }
        return !patternSlots.hasNext() && minRem == 0
    }

    fun evaluate(captures: Array<List<MatchResult>>): String {
        val builder = StringBuilder()
        val sz = slots.size
        var i = 0 // #slot
        var c = 0 // #capture
        var ci = 0 // #capture index
        builder.append(literals[i])
        if (captures.isEmpty()) return builder.toString()
        var capture = captures[c++]
        Outer@ while (i < sz) {
            val slot = slots[i]
            var joiner = false
            var consumed = 0
            while (consumed < slot.count) {
                if (ci == capture.size) {
                    if (c == captures.size) break@Outer
                    ci = 0
                    capture = captures[c++]
                    if (slot.sameMatcher) break
                }
                if (joiner) builder.append(slot.delimiter)
                joiner = true
                slot.transform(capture[ci], builder)
                consumed++
                ci++
            }
            i++
            builder.append(literals[i])
        }
        return builder.toString()
    }

    companion object {
        @Throws(ParseException::class)
        fun parse(s: String): UnicodeJoiner {
            var i = 0
            var c: Char
            val sz = s.length
            val expr = UnicodeJoiner()
            var currentLiteral = 0
            while (i < sz) {
                c = s[i]
                if (c == '{') {
                    if (i == 0) expr.literals.add("") else expr.literals.add(s.substring(currentLiteral, i))
                    val start = i
                    i = s.indexOf('}', start)
                    if (i == -1) throw ParseException("Unclosed Slot: $s", start)
                    val content = s.substring(start + 1, i)
                    expr.slots.add(parse(content, start))
                    currentLiteral = i + 1
                }
                i++
            }
            expr.literals.add(s.substring(currentLiteral, i))
            return expr
        }
    }
}