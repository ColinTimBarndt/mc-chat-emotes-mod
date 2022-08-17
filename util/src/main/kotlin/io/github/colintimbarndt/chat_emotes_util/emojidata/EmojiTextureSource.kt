package io.github.colintimbarndt.chat_emotes_util.emojidata

enum class EmojiTextureSource(private val label: String, val loader: TextureLoader, val license: TextureRights) {
    // Emoji Data Atlas
    Apple(
        "Apple",
        TextureLoader.emojiDataSpritesheet("apple"),
        TextureRights.COPYRIGHT
    ),
    Google(
        "Google (Noto)",
        TextureLoader.emojiDataSpritesheet("google"),
        TextureRights.GOOGLE_NOTO
    ),
    Twitter(
        "Twitter (Twemoji)",
        TextureLoader.emojiDataSpritesheet("twitter"),
        TextureRights.TWITTER_TWEMOJI
    ),
    Facebook(
        "Facebook",
        TextureLoader.emojiDataSpritesheet("facebook"),
        TextureRights.COPYRIGHT
    ),
    // Zipped
    Openmoji(
        "OpenMoji",
        TextureLoader.openmoji(),
        TextureRights.OPENMOJI
    );
    //Joypixels(
    //    "JoyPixels",
    //    /* REQUIRES LOGIN */,
    //    TextureRights.JOYPIXELS_FREE
    //);

    override fun toString() = label

    companion object {
        val default = Twitter
    }
}