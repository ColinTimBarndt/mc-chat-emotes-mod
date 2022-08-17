package io.github.colintimbarndt.chat_emotes_util.emojidata

enum class LicenseKind(private val label: String) {
    PersonalUse("Personal Use License"),
    CreativeCommons("Creative Commons License"),
    OpenFontLicense("Open Font License"),
    Copyright("Copyright");

    override fun toString() = label
}