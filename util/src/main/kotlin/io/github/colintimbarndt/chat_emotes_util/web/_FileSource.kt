@file:JvmName("FileSourceKt")

package io.github.colintimbarndt.chat_emotes_util.web

import io.github.colintimbarndt.chat_emotes_util.model.UriSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import java.net.URI

private val jsonIgnoreUnknownKeys = Json {
    ignoreUnknownKeys = true
}

@Serializable
sealed interface FileSource {
    val uri: URI
    val userUri get() = uri
}

@JvmInline
@Serializable
@SerialName("uri")
value class FileUri(
    @SerialName("value")
    @Serializable(UriSerializer::class)
    override val uri: URI
) : FileSource {
    constructor(value: String) : this(URI(value))
}

@Serializable
@SerialName("github:release")
data class GithubRelease(
    val owner: String,
    val repo: String,
    val tag: String,
    val file: String,
) : FileSource {
    @Transient
    override val uri = URI(
        "https://github.com/$owner/$repo/releases/" +
                if (tag == "latest") "latest/download/$file"
                else "download/$tag/$file"
    )
    override val userUri
        get() = URI(
            "https://github.com/$owner/$repo/releases/" +
                    if (tag == "latest") "latest"
                    else "tag/$tag"
        )
}

@Serializable
@SerialName("github:file")
data class GithubFile(
    val owner: String,
    val repo: String,
    val branch: String,
    val path: String,
) : FileSource {
    @Transient
    override val uri = URI("https://raw.githubusercontent.com/$owner/$repo/$branch/$path")
    override val userUri get() = URI("https://github.com/$owner/$repo/blob/$branch/$path")

    suspend fun getInputStream() = WebHelper.getInputStream(uri)

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getCommitHash(): String {
        @Serializable
        data class Commit(
            val sha: String,
        )

        val commitsUri = URI("https://api.github.com/repos/$owner/$repo/commits?path=$path&per_page=1")
        val response = WebHelper.getInputStream(commitsUri)

        if (response.statusCode() !in 200 until 400) {
            throw WebHelper.HttpStatusException(commitsUri, response.statusCode())
        }

        val commits = jsonIgnoreUnknownKeys.decodeToSequence<Commit>(response.body(), DecodeSequenceMode.ARRAY_WRAPPED)
        return commits.first().sha
    }
}