package io.github.colintimbarndt.chat_emotes_util.model

import kotlinx.serialization.Serializable

@Serializable(ResourceKeySerializer::class)
data class ResourceKey(
    val namespace: String,
    val path: String,
) {
    override fun toString() = "$namespace:$path"

    companion object {
        fun of(str: String): ResourceKey {
            val idx = str.indexOf(':')
            return if (idx < 0) ResourceKey("minecraft", str)
            else ResourceKey(str.substring(0 until idx), str.substring(idx + 1))
        }
    }
}