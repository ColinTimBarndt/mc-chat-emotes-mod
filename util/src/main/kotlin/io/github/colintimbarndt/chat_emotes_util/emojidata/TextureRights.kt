package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.AsURI
import io.github.colintimbarndt.chat_emotes_util.WebHelper
import java.net.URI

sealed class TextureRights(
    val kind: LicenseKind,
) {
    abstract val source: URI?
    abstract val message: String

    class License(override val source: URI, kind: LicenseKind) : TextureRights(kind) {
        constructor(source: AsURI, kind: LicenseKind) : this(source.uri, kind)
        override val message get() = "This work is licensed under a $kind"
    }

    object Copyright : TextureRights(LicenseKind.Copyright) {
        override val source: URI? = null
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
            URI("https://cdn.joypixels.com/free-license.pdf"),
            LicenseKind.PersonalUse
        )
    }
}
