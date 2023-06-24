package io.github.colintimbarndt.chat_emotes.common.data

interface EmoteDataSource {
    val loadedEmoteData: List<EmoteDataBundle>
    val aliasTree: AliasPrefixTree
    val emojiTree: EmojiPrefixTree
}