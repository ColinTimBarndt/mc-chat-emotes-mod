@file:JvmName("FileSourceTemplateKt")

package io.github.colintimbarndt.chat_emotes_util.web

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.regex.Pattern

@Serializable
sealed interface FileSourceTemplate {
    fun resolveTemplate(resolve: (String) -> String): FileSource
}

private val templatePattern = Pattern.compile("\\{([a-zA-Z0-9_-]+)}")

private fun resolveTemplate(str: String, resolve: (String) -> String) =
    templatePattern.matcher(str).replaceAll {
        resolve(it.group(1))
    }

@JvmInline
@Serializable
@SerialName("uri")
value class FileUriTemplate(
    private val value: String
) : FileSourceTemplate {
    override fun resolveTemplate(resolve: (String) -> String) = FileUri(resolveTemplate(value, resolve))
}

@Serializable
@SerialName("match")
data class MatchingFileSourceTemplate(
    private val on: String,
    private val cases: Map<String, FileSourceTemplate>
) : FileSourceTemplate {
    override fun resolveTemplate(resolve: (String) -> String): FileSource {
        val on = resolveTemplate(on, resolve)
        return cases[on]!!.resolveTemplate(resolve)
    }
}

@Serializable
@SerialName("github:release")
data class GithubReleaseTemplate(
    val owner: String,
    val repo: String,
    val tag: String,
    val file: String,
) : FileSourceTemplate {
    override fun resolveTemplate(resolve: (String) -> String) = GithubRelease(
        owner = resolveTemplate(owner, resolve),
        repo = resolveTemplate(repo, resolve),
        tag = resolveTemplate(tag, resolve),
        file = resolveTemplate(file, resolve)
    )
}

@Serializable
@SerialName("github:file")
data class GithubFileTemplate(
    val owner: String,
    val repo: String,
    val branch: String,
    val path: String,
) : FileSourceTemplate {
    override fun resolveTemplate(resolve: (String) -> String) = GithubFile(
        owner = resolveTemplate(owner, resolve),
        repo = resolveTemplate(repo, resolve),
        branch = resolveTemplate(branch, resolve),
        path = resolveTemplate(path, resolve)
    )
}