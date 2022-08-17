package io.github.colintimbarndt.chat_emotes_util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler

object WebHelper {
    private val client = HttpClient.newHttpClient()
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

    suspend fun getInputStream(uri: URI) = get(uri, HttpResponse.BodyHandlers.ofInputStream())

    val <T> HttpResponse<T>.contentLength: Long get() =
        headers().firstValueAsLong("content-length").orElse(0)

    class HttpStatusException(uri: URI, status: Int) : Exception("Request $uri returned status $status")

    @Serializable
    data class GithubFile(
        val owner: String,
        val repo: String,
        val branch: String,
        val path: String,
    ) : AsURI {
        @Transient
        override val uri = URI("https://github.com/$owner/$repo/raw/$branch/$path")

        suspend fun getInputStream() = getInputStream(uri)

        @OptIn(ExperimentalSerializationApi::class)
        suspend fun getCommitHash(): String {
            @Serializable
            data class Commit(
                val sha: String,
            )

            val commitsUri = URI("https://api.github.com/repos/$owner/$repo/commits?path=$path&per_page=1")
            val response = getInputStream(commitsUri)

            if (response.statusCode() !in 200 until 400) {
                throw HttpStatusException(commitsUri, response.statusCode())
            }

            val commits = json.decodeToSequence<Commit>(response.body(), DecodeSequenceMode.ARRAY_WRAPPED)
            return commits.first().sha
        }
    }
}