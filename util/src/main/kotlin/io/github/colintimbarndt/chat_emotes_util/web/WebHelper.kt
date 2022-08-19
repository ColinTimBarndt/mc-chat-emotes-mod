package io.github.colintimbarndt.chat_emotes_util.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler

object WebHelper {
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private const val userAgentString = "ChatEmotesUtil/1.0 (github ColinTimBarndt/mc-chat-emotes-mod)"

    suspend fun <T> get(uri: URI, handler: BodyHandler<T>): HttpResponse<T> {
        val request = HttpRequest.newBuilder(uri).GET()
            .header("User-Agent", userAgentString)
            .build()
        return withContext(Dispatchers.IO) {
            client.send(request, handler)
        }
    }

    suspend fun getInputStream(uri: URI): HttpResponse<InputStream> =
        get(uri, HttpResponse.BodyHandlers.ofInputStream())

    val <T> HttpResponse<T>.contentLength: Long
        get() =
            headers().firstValueAsLong("content-length").orElse(0)

    class HttpStatusException(uri: URI, status: Int) : Exception("Request $uri returned status $status")

}