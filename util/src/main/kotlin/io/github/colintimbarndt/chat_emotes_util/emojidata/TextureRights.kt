package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.WebHelper.FileSource
import io.github.colintimbarndt.chat_emotes_util.WebHelper

sealed class TextureRights(
    val kind: LicenseKind,
) {
    abstract val source: FileSource?
    abstract val message: String

    class License(override val source: FileSource, kind: LicenseKind) : TextureRights(kind) {
        override val message get() = "This work is licensed under a $kind"
    }

    object Copyright : TextureRights(LicenseKind.Copyright) {
        override val source: FileSource? = null
        override val message get() = "This work is copyrighted. Permission must be granted by the copyright holder"
    }

    companion object {
        val COPYRIGHT = Copyright
        val GOOGLE_NOTO = License(
            WebHelper.GithubFile("googlefonts", "noto-emoji", "main", "LICENSE"),
            LicenseKind.OpenFontLicense
        )
        val TWITTER_TWEMOJI = License(
            WebHelper.GithubFile("twitter", "twemoji", "master", "LICENSE_GRAPHICS"),
            LicenseKind.CreativeCommons
        )
        val OPENMOJI = License(
            WebHelper.GithubFile("hfg-gmuend", "openmoji", "master", "LICENSE.txt"),
            LicenseKind.CreativeCommons
        )
        val JOYPIXELS_FREE = License(
            WebHelper.FileUri("https://cdn.joypixels.com/free-license.pdf"),
            LicenseKind.PersonalUse
        )
    }
}
