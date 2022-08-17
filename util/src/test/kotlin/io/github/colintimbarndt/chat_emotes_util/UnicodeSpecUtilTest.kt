package io.github.colintimbarndt.chat_emotes_util

import io.github.colintimbarndt.chat_emotes_util.UnicodeSpecUtil.EmojiVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class UnicodeSpecUtilTest {
    @Test
    fun parseVersionsHTML() {
        val document = UnicodeSpecUtilTest::class.java.getResourceAsStream("/assets/test/dir_response.html")!!
        val mockResponse = HttpMockResponse(document)
        val res = UnicodeSpecUtil.handleEmojiVersionsResponse(mockResponse, TreeSet())
        assertEquals(res, setOf(
            EmojiVersion(1, 0),
            EmojiVersion(2, 0),
            EmojiVersion(3, 0),
            EmojiVersion(4, 0),
            EmojiVersion(5, 0),
            EmojiVersion(11, 0),
            EmojiVersion(12, 0),
            EmojiVersion(12, 1),
            EmojiVersion(13, 0),
            EmojiVersion(13, 1),
            EmojiVersion(14, 0),
            EmojiVersion(15, 0),
        ))
    }
}