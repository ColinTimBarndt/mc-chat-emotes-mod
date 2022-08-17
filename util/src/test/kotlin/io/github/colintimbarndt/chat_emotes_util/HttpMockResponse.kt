package io.github.colintimbarndt.chat_emotes_util

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import javax.net.ssl.SSLSession

class HttpMockResponse<T>(private val body: T) : HttpResponse<T> {
    override fun statusCode() = 200

    override fun request(): HttpRequest = throw UnsupportedOperationException()

    override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()

    private val headers = HttpHeaders.of(mutableMapOf()) { _, _ -> true }
    override fun headers(): HttpHeaders = headers

    override fun body(): T = body

    override fun sslSession(): Optional<SSLSession> = Optional.empty()

    private val uri = URI("http://example.com")
    override fun uri(): URI = uri

    override fun version(): HttpClient.Version = HttpClient.Version.HTTP_2
}