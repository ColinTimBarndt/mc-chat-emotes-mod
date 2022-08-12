package io.github.colintimbarndt.chat_emotes.common.data.unicode

import java.util.*
import java.util.regex.Pattern

class UnicodeModifierType(val name: String, val priority: Int, val defaultChoice: Int, val format: String) :
    Comparable<UnicodeModifierType> {

    var values: Array<Modifier> = EMPTY_MODIFIERS
    private var replace: Array<Pattern>? = null

    override fun compareTo(other: UnicodeModifierType): Int {
        return priority.compareTo(other.priority)
    }

    inner class Modifier(val sequence: String, val ordinal: Int, val names: Array<String>) : Comparable<Modifier> {
        val type: UnicodeModifierType
            get() = this@UnicodeModifierType

        fun getModifiedName(alias: String): String {
            val replace0 = replace
            if (replace0 != null) {
                for (j in replace0.indices) {
                    val pattern = replace0[j]
                    val matcher = pattern.matcher(alias)
                    if (matcher.find()) {
                        return matcher.replaceFirst(names[j])
                    }
                }
            }
            return String.format(format, alias, names[defaultChoice])
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val modifier = other as Modifier
            return (ordinal == modifier.ordinal
                    && type == modifier.type)
        }

        override fun hashCode(): Int {
            return ordinal + 15 * type.hashCode()
        }

        override fun compareTo(other: Modifier): Int {
            return this@UnicodeModifierType.compareTo(other.type)
        }
    }

    class Builder(name: String, defaultChoice: Int, format: String) {
        private val sequences: MutableSet<String> = HashSet()
        private val names: MutableSet<String> = HashSet()
        private val modifiers = ArrayList<Modifier>(8)
        private val replace = ArrayList<Pattern>(0)
        private val type: UnicodeModifierType
        private var ordinal = 0
        private var priority = 0
        private var isBuilt = false

        init {
            type = UnicodeModifierType(name, priority, defaultChoice, format)
        }

        fun create(): UnicodeModifierType {
            check(!isBuilt) { "already built" }
            isBuilt = true
            if (replace.isNotEmpty()) {
                for (mod in modifiers) {
                    check(mod.names.size == replace.size) {
                        "length of replace is not equal to the length of all names arrays of the modifiers"
                    }
                }
                type.replace = replace.toTypedArray()
            }
            type.values = modifiers.toTypedArray()
            return type
        }

        fun priority(prio: Int): Builder {
            priority = prio
            return this
        }

        fun replace(pattern: Pattern): Builder {
            check(!isBuilt) { "already built" }
            replace.add(Objects.requireNonNull(pattern))
            return this
        }

        fun addVariant(sequence: String, names: Array<String>): Builder {
            check(!isBuilt) { "already built" }
            check(!sequences.contains(sequence)) { "sequence already registered" }
            check(!Arrays.stream(names).anyMatch(::isNameUsed)) { "name already in use" }
            sequences += sequence
            this.names += names
            modifiers += type.Modifier(sequence, ordinal++, names)
            return this
        }

        fun isNameUsed(name: String): Boolean {
            return names.contains(name)
        }
    }

    companion object {
        fun build(name: String, defaultChoice: Int, format: String): Builder {
            return Builder(name, defaultChoice, format)
        }

        private val EMPTY_MODIFIERS: Array<Modifier> = emptyArray()
    }
}