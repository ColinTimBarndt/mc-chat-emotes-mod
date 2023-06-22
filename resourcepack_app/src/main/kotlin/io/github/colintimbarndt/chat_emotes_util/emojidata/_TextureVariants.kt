@file:JvmName("TextureVariantsKt")
@file:OptIn(ExperimentalSerializationApi::class)

package io.github.colintimbarndt.chat_emotes_util.emojidata

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("rights_type")
sealed interface TextureVariants {
    val values: Collection<String>
    fun usageRightsFor(value: String): TextureUsageRights
    fun shortNameFor(value: String): String?
    fun dataFor(value: String): Map<String, String>
}

@Serializable
@SerialName("equal")
data class EqualRightsTextureVariants(
    private val rights: TextureUsageRights,
    private val variants: ArrayList<Entry>,
) : TextureVariants {
    @Transient
    override val values = variants.map(Entry::name)
    override fun usageRightsFor(value: String) = rights
    override fun shortNameFor(value: String) = this[value]!!.shortName
    override fun dataFor(value: String): Map<String, String> = this[value]!!.data

    private operator fun get(value: String) = variants.find { it.name == value }

    @Serializable
    data class Entry(
        val name: String,
        @SerialName("short_name")
        val shortName: String? = null,
        val data: Map<String, String> = emptyMap()
    )
}

@Serializable
@SerialName("individual")
data class IndividualRightsTextureVariants(
    @SerialName("values")
    private val map: Map<String, Entry>,
) : TextureVariants {
    override val values get() = map.keys
    override fun usageRightsFor(value: String) = map[value]!!.rights
    override fun shortNameFor(value: String) = map[value]!!.shortName
    override fun dataFor(value: String) = map[value]!!.data

    @Serializable
    data class Entry(
        @SerialName("short_name")
        val shortName: String? = null,
        val rights: TextureUsageRights,
        val data: Map<String, String> = emptyMap()
    )
}