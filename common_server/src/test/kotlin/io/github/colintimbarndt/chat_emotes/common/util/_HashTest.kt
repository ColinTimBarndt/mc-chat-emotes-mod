@file:JvmName("HashTestKt")

package io.github.colintimbarndt.chat_emotes.common.util

import io.github.colintimbarndt.chat_emotes.common.util.HashedStringBuilder.Companion.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HashTest {
    @Test
    fun simpleHashTest() {
        val str = "Hello World!"
        val expect = str.hashCode()
        val builder = HashedStringBuilder(str.length)
        builder.append(str)
        assertEquals(expect, builder.hashCode(), "hash equals")
        assertTrue(builder.equals(str), "equals")
    }

    @Test
    fun concatHashTest() {
        val str1 = "Hello"
        val str2 = " World!"
        val concat = str1 + str2
        val expect = concat.hashCode()
        val builder = HashedStringBuilder(concat.length)
        builder.append(str1)
        builder.append(str2)
        assertEquals(expect, builder.hashCode(), "hash equals")
        assertTrue(builder.equals(concat), "equals")
    }

    @Test
    fun lookupTest() {
        val map = hashMapOf(
            "test string xyz" to "A",
            "1" to "B",
            "2" to "C",
        )
        val str = map[HashedStringBuilder("test string xyz")]
        assertEquals("A", str)
    }
}