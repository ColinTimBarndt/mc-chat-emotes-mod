package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.web.FileSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface TextureUsageRights {
    val kind: LicenseKind
    val message: String

    fun sources(): Sequence<FileSource>

    @Serializable
    @SerialName("license")
    class License(
        val source: FileSource,
        override val kind: LicenseKind
        ) : TextureUsageRights {
        override fun sources() = sequenceOf(source)
        override val message get() =
            "This work is licensed under a $kind. You may use it under the given conditions"
    }

    @Serializable
    @SerialName("copyright")
    data class Copyright(
        val holder: String
    ) : TextureUsageRights {
        @Transient
        override val kind: LicenseKind = LicenseKind.Copyright
        override fun sources() = sequenceOf<FileSource>()
        override val message get() =
            "This work is copyrighted. Permission must be granted by the copyright holder $holder"
    }

    @Serializable
    @SerialName("multiple")
    data class Multiple(
        private val values: ArrayList<TextureUsageRights>
    ) : TextureUsageRights {
        @Transient
        override val kind: LicenseKind = LicenseKind.Multiple
        override fun sources() = sequence {
            for (entry in values) {
                yieldAll(entry.sources())
            }
        }

        override val message get() =
            values.asSequence()
                .map(TextureUsageRights::message)
                .joinToString("\n")
    }

    @Serializable
    @SerialName("unknown")
    object Unknown : TextureUsageRights {
        @Transient
        override val kind: LicenseKind = LicenseKind.Unknown
        @Transient
        override val message = "These textures may contain elements with an unknown license"
        override fun sources() = sequenceOf<FileSource>()
    }
}
