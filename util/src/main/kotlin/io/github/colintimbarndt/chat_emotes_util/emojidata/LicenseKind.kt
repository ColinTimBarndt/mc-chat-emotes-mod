package io.github.colintimbarndt.chat_emotes_util.emojidata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LicenseKind(private val label: String) {
    @SerialName("personal use")
    PersonalUse("Personal Use License"),
    @SerialName("creative commons")
    CreativeCommons("Creative Commons License"),
    @SerialName("open font license")
    OpenFontLicense("Open Font License"),
    @Transient
    Unknown("Unknown"),
    @Transient
    Multiple("Multiple Licenses"),
    @Transient
    Copyright("Copyright")
    ;

    override fun toString() = label
}