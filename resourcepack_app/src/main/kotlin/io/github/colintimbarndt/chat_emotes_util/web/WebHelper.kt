package io.github.colintimbarndt.chat_emotes_util.web

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

object WebHelper {
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
    private const val userAgentString = "ChatEmotesUtil/1.0 (github ColinTimBarndt/mc-chat-emotes-mod)"
    private val cacheDir: File =
        WebHelper::class.java.protectionDomain.codeSource.location.file.let(::File).resolve("ChatEmotesUtil.cache")

    private val manifestFile = cacheDir.resolve("manifest.json")
    private val cacheManifest: ConcurrentHashMap<String, CacheManifestEntry>
    private val json = Json { prettyPrint = true }

    const val STANDARD_CACHE_TIME = 60L * 60L * 1000L /* one hour */

    init {
        LOGGER.info("Caching files at ${cacheDir.absolutePath}")
        cacheDir.mkdir()

        @OptIn(ExperimentalSerializationApi::class)
        cacheManifest = if (manifestFile.isFile) {
            ConcurrentHashMap<String, CacheManifestEntry>(
                json.decodeFromStream<Map<String, CacheManifestEntry>>(manifestFile.inputStream().buffered())
            )
        } else {
            ConcurrentHashMap()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Synchronized
    private fun saveCacheManifest() {
        try {
            manifestFile.outputStream().use {
                json.encodeToStream<Map<String, CacheManifestEntry>>(cacheManifest, it)
            }
        } catch (ex: IOException) {
            LOGGER.error("Failed to save cache manifest")
        }
    }

    fun <T> get(uri: URI, handler: CachedBodyHandler<T>, cacheTime: Long): FetchResult<out T> {
        val mfEntry = if (cacheTime > 0) cacheManifest[uri.toASCIIString()]?.let { mfEntry ->
            if (System.currentTimeMillis() - mfEntry.lastUpdate < cacheTime) {
                val cached = cacheDir.resolve(mfEntry.file)
                if (cached.isFile) {
                    LOGGER.info("Loading from cache $uri")
                    return handler.readFromCache(cached)
                } else {
                    cacheManifest.remove(uri.toASCIIString())
                    saveCacheManifest()
                    return@let null
                }
            }
            mfEntry
        } else null

        val request = HttpRequest.newBuilder(uri).GET().header("User-Agent", userAgentString)

        if (mfEntry?.etag != null) request.header("If-None-Match", mfEntry.etag)

        val result = client.send(request.build(), handler)
        if (result.statusCode() == 304 /* Not Modified */) {
            if (mfEntry == null) throw HttpStatusException(uri, 304)
            val cached = cacheDir.resolve(mfEntry.file)
            if (cached.isFile) {
                mfEntry.lastUpdate = System.currentTimeMillis()
                saveCacheManifest()
                LOGGER.info("Loading from cache $uri")
                return handler.readFromCache(cached)
            } else {
                cacheManifest.remove(uri.toASCIIString())
                saveCacheManifest()
            }
        }
        LOGGER.info("Downloading file $uri")
        return if (cacheTime > 0) {
            var file: File
            if (mfEntry != null) file = cacheDir.resolve(mfEntry.file)
            else do {
                file = cacheDir.resolve(UUID.randomUUID().toString())
            } while (file.exists())
            handler.writeToCache(result, file)
            val time = System.currentTimeMillis()

            @OptIn(ExperimentalStdlibApi::class)
            val etag = result.headers().firstValue("etag").getOrNull()
            if (mfEntry != null) {
                mfEntry.lastUpdate = time
                mfEntry.etag = etag
            } else {
                cacheManifest += uri.toASCIIString() to CacheManifestEntry(
                    file = file.name,
                    lastUpdate = time,
                    etag = etag
                )
            }
            saveCacheManifest()
            handler.readFromCache(file)
        } else {
            FetchResult(result)
        }
    }

    suspend fun getInputStream(uri: URI, cacheTime: Long = 0L) = withContext(Dispatchers.IO) {
        get(uri, CachedInputStreamBodyHandler(), cacheTime)
    }

    fun getInputStreamSync(uri: URI, cacheTime: Long = 0L) =
        get(uri, CachedInputStreamBodyHandler(), cacheTime)

    val <T> HttpResponse<T>.contentLength: Long
        get() = headers().firstValueAsLong("content-length").orElse(0)

    class HttpStatusException(uri: URI, status: Int) : Exception("Request $uri returned status $status")

    @Serializable
    private data class CacheManifestEntry(
        val file: String,
        @SerialName("last_update") var lastUpdate: Long,
        var etag: String? = null,
    )

    data class FetchResult<T>(
        val result: T,
        val contentLength: Long,
        val statusCode: Int,
    ) {
        constructor(http: HttpResponse<T>) : this(http.body(), http.contentLength, http.statusCode())
    }

    abstract class CachedBodyHandler<T>(private val httpHandler: BodyHandler<T>) : BodyHandler<T> by httpHandler {
        abstract fun readFromCache(cacheFile: File): FetchResult<out T>
        abstract fun writeToCache(result: HttpResponse<T>, cacheFile: File)
    }

    private class CachedInputStreamBodyHandler :
        CachedBodyHandler<InputStream>(HttpResponse.BodyHandlers.ofInputStream()) {
        override fun readFromCache(cacheFile: File) = FetchResult(
            cacheFile.inputStream().buffered(), cacheFile.length(), 304
        )

        override fun writeToCache(result: HttpResponse<InputStream>, cacheFile: File) {
            cacheFile.outputStream().buffered().use(result.body()::transferTo)
        }
    }
}