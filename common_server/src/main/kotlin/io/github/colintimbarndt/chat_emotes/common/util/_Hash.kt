@file:JvmName("HashKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes.common.util

private val pow31Lut = Array(32) { 0 }.apply {
    var x = 1
    for (i in indices) {
        this[i] = x
        x *= 31
    }
}

private inline fun pow31(e: Int): Int {
    if (e < 32) return pow31Lut[e]
    var result = pow31Lut[31]
    for (x in 0 until e - 31) result *= 31
    return result
}

class HashedStringBuilder(
    private val sb: StringBuilder = StringBuilder()
) {
    constructor(capacity: Int) : this(StringBuilder(capacity))

    constructor(str: String) : this(StringBuilder(str)) {
        hash = str.hashCode()
    }

    constructor(str: String, from: Int, to: Int) : this(StringBuilder(to - from)) {
        append(str, from, to)
    }

    private var hash: Int = 0

    val length get() = sb.length

    operator fun get(idx: Int) = sb[idx]

    fun append(char: Char) {
        sb.append(char)
        hash = hash * 31 + char.code
    }

    fun append(str: String) {
        sb.append(str)
        /*
        hash(str) := sum for(n in 0 until str.length) str[0] * 31^(length - (n + 1))

        let str1, str2 instanceof String
        hash(str1 + str2) =
            let hash1 := hash(str1)
            return hash1 * 31^(str2.length - 1) + hash(str2)
         */
        hash = hash * pow31(str.length) + str.hashCode()
    }

    fun append(str: String, from: Int, to: Int) {
        sb.append(str, from, to)
        for (i in from until to) {
            val char = str[i]
            hash = hash * 31 + char.code
        }
    }

    override fun hashCode() = hash

    override fun equals(other: Any?): Boolean {
        other ?: return false
        return when (other.javaClass) {
            String::class.java -> {
                other as String
                other.contentEquals(sb)
            }

            HashedStringBuilder::class.java -> {
                other as HashedStringBuilder
                hash == other.hash && sb == other.sb
            }

            else -> false
        }
    }

    override fun toString() = sb.toString()

    companion object {
        /**
         * **IMPORTANT**: For this to work, the map must be or delegate to a [HashMap]
         */
        @Suppress("UNCHECKED_CAST")
        inline operator fun <V> Map<String, V>.get(idx: HashedStringBuilder): V? = (this as Map<Any, V>)[idx]
    }
}
