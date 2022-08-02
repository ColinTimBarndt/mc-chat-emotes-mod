package io.github.colintimbarndt.chat_emotes.data

import net.minecraft.resources.ResourceLocation
import java.io.IOException
import java.nio.file.Path

interface EmoteData {
    val location: ResourceLocation
    val emotes: Set<Emote>

    /**
     * @return [Set] of all aliases this data covers
     */
    val aliases: Set<String>

    /**
     * @return [Set] of all emoticons this data covers
     */
    val emoticons: Set<String>
    fun emoteForUnicodeSequence(sequence: String): Emote?
    fun emoteForAlias(alias: String): Emote?
    fun emoteForEmoticon(emoticon: String): Emote?
    val serializer: EmoteDataSerializer<*>

    @Throws(IOException::class)
    fun generateFonts(gen: FontGenerator, imageSources: Path)
}