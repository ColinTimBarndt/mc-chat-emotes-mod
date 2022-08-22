package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.web.FileSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Suppress("UNUSED")
sealed interface TextureUsageRights {
    val kind: UsageRightsKind
    val message: String

    fun sources(): Sequence<FileSource>

    @Serializable
    @SerialName("license")
    class License(
        val source: FileSource,
        override val kind: UsageRightsKind
    ) : TextureUsageRights {
        override fun sources() = sequenceOf(source)
        override val message
            get() =
                "This pack contains textures licensed under the $kind. You may use it under the given conditions"
    }

    @Serializable
    @SerialName("copyright")
    data class Copyright(
        val holder: String
    ) : TextureUsageRights {
        @Transient
        override val kind: UsageRightsKind = UsageRightsKind.Copyright
        override fun sources() = sequenceOf<FileSource>()
        override val message
            get() =
                "This pack contains copyrighted textures. Permission must be granted by the copyright holder $holder"
    }

    @Serializable
    @SerialName("multiple")
    data class Multiple(
        private val values: ArrayList<TextureUsageRights>
    ) : TextureUsageRights {
        @Transient
        override val kind: UsageRightsKind = UsageRightsKind.Multiple
        override fun sources() = sequence {
            for (entry in values) {
                yieldAll(entry.sources())
            }
        }

        override val message
            get() =
                values.asSequence()
                    .map(TextureUsageRights::message)
                    .joinToString("\n")
    }

    @Serializable
    @SerialName("unknown")
    object Unknown : TextureUsageRights {
        @Transient
        override val kind: UsageRightsKind = UsageRightsKind.Unknown

        @Transient
        override val message = "This pack may contain textures with unknown usage rights"
        override fun sources() = sequenceOf<FileSource>()
    }
}
