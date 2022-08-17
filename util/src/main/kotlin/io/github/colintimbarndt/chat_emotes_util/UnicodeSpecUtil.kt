package io.github.colintimbarndt.chat_emotes_util

import io.karma.sliced.View
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator

// TEMPORARY, sliced will get updated soon
typealias StringSlice = View<Char>

object UnicodeSpecUtil {
    const val USER_AGENT = "ChatEmotesUtil/1.0"
    val EMOJI_SPEC_BASE_URI = URI("https://unicode.org/Public/emoji/")

    private var cachedVersions: TreeSet<EmojiVersion>? = null
    private val http = HttpClient.newHttpClient()
    private val versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)/?$")
    private val parserDelegator = ParserDelegator()

    data class EmojiVersion(val major: Int, val minor: Int) : Comparable<EmojiVersion> {
        override fun compareTo(other: EmojiVersion): Int {
            val r = major.compareTo(other.major)
            return if (r == 0) minor.compareTo(other.minor)
            else r
        }

        override fun toString() = "$major.$minor"
    }

    fun getEmojiVersions(): CompletableFuture<out SortedSet<EmojiVersion>>? {
        if (cachedVersions != null) return CompletableFuture.completedFuture(cachedVersions!!)

        val versions = TreeSet<EmojiVersion>()
        return getHTMLAsync(EMOJI_SPEC_BASE_URI).thenApply { response ->
            handleEmojiVersionsResponse(response, versions)
        }.exceptionally { error ->
            System.err.println("unable to read $EMOJI_SPEC_BASE_URI")
            error.printStackTrace()
            versions
        }
    }

    fun getAllEmojis(version: EmojiVersion): CompletableFuture<Flow<String>> {
        val versionUri = EMOJI_SPEC_BASE_URI.resolve("$version/")
        return getHTMLAsync(versionUri).thenApply { dirResponse ->
            val scan = ArrayList<String>(2)
            val reader = InputStreamReader(dirResponse.body())
            parserDelegator.parse(reader, HTMLDirVisitor { href ->
                when (href) {
                    "emoji-data.txt",
                    "emoji-sequences.txt",
                    "emoji-zwj-sequences.txt" -> scan += href

                    else -> {}
                }
            }, false)

            flow {
                val buffer = CharArray(16)
                scan.forEach { fileName ->
                    val fileUri = versionUri.resolve(fileName)
                    val request = HttpRequest.newBuilder()
                        .GET().uri(fileUri)
                        .header("Accept", "text/plain")
                        .header("User-Agent", USER_AGENT)
                        .build()
                    val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
                    val content = response.body()
                        .let(::InputStreamReader)
                        .let(::BufferedReader)
                        .lineSequence()
                        .map(::stripComments)
                        .filter(String::isNotEmpty)
                    for (line in content) {
                        val sep = line.indexOf(';')
                        val dot = line.indexOf('.')
                        if (dot < sep) {
                            if (line[dot + 1] != '.') continue
                            // from..to
                            val fromCp = line.substring(0 until dot).toInt(16)
                            val toCp = line.substring(dot + 2 until sep).toInt(16)
                            for (cp in fromCp..toCp) {
                                emit(Character.toString(cp))
                            }
                        } else {
                            if (dot != -1) continue
                            // codepoints
                            val builder = StringBuilder(32)
                            for (cp in line.splitToSequence(' ').map(String::toInt)) {
                                builder.appendCodePoint(cp)
                            }
                            emit(builder.toString())
                        }
                    }
                }
            }
        }
    }

    /**
     * TODO: Replace with Views feature once available
     */
    private fun stripComments(s: String): String {
        val idx = s.indexOf('#')
        return if (idx == -1) s
        else s.substring(0 until idx)
    }

    private fun getHTMLAsync(uri: URI): CompletableFuture<HttpResponse<InputStream>> {
        val request = HttpRequest.newBuilder()
            .GET().uri(uri)
            .header("Accept", "text/html")
            .header("User-Agent", USER_AGENT)
            .build()
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
    }

    internal fun handleEmojiVersionsResponse(
        response: HttpResponse<InputStream>,
        versions: TreeSet<EmojiVersion>
    ): SortedSet<EmojiVersion> {
        val reader = InputStreamReader(response.body())
        parserDelegator.parse(reader, HTMLDirVisitor { href ->
            parseEmojiVersion(href)?.let(versions::add)
        }, false)
        cachedVersions = versions
        return versions
    }

    fun parseEmojiVersion(href: String): EmojiVersion? {
        val matcher = versionPattern.matcher(href)
        return if (matcher.matches()) {
            EmojiVersion(matcher.group(1).toInt(), matcher.group(2).toInt())
        } else null
    }

    private class HTMLDirVisitor(val acceptHref: (String) -> Unit) : HTMLEditorKit.ParserCallback() {
        override fun handleStartTag(tag: HTML.Tag, attrs: MutableAttributeSet, pos: Int) {
            if (tag != HTML.Tag.A) return
            val href = attrs.getAttribute(HTML.Attribute.HREF)
            if (href is String && !href.startsWith('/')) {
                acceptHref(href)
            }
        }
    }
}
