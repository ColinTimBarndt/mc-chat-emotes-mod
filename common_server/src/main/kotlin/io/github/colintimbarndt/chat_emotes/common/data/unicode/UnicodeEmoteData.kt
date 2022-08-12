package io.github.colintimbarndt.chat_emotes.common.data.unicode

import io.github.colintimbarndt.chat_emotes.common.data.Emote
import io.github.colintimbarndt.chat_emotes.common.data.EmoteData
import io.github.colintimbarndt.chat_emotes.common.data.FontGenerator
import net.minecraft.resources.ResourceLocation
import java.io.IOException
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO

class UnicodeEmoteData internal constructor(override val location: ResourceLocation) : EmoteData {
    internal val emotesMut: MutableSet<Emote> = HashSet()
    val emotesByUnicodeSequence: MutableMap<String, Emote> = TreeMap()
    val emotesByAlias: MutableMap<String, Emote> = HashMap()
    val emotesByEmoticon: MutableMap<String, Emote> = HashMap()

    override val emotes: Set<Emote> get() = emotesMut

    override val aliases: Set<String> get() = emotesByAlias.keys

    override val emoticons: Set<String> get() = emotesByEmoticon.keys

    override fun emoteForUnicodeSequence(sequence: String): Emote? {
        return emotesByUnicodeSequence[sequence]
    }

    override fun emoteForAlias(alias: String): Emote? {
        return emotesByAlias[alias]
    }

    override fun emoteForEmoticon(emoticon: String): Emote? {
        return emotesByEmoticon[emoticon]
    }

    override val serializer = UnicodeEmoteDataSerializer

    @Throws(IOException::class)
    override fun generateFonts(gen: FontGenerator, imageSources: Path) {
        // TODO: optimize
        val fonts = HashMap<ResourceLocation, FontGenerator.Font>(8)
        try {
            EmoteTextureArchive(imageSources.toFile()).use { images ->
                for (emote in emotesMut) {
                    val seq = emote.unicodeSequence ?: continue
                    val texture = images.getTextureAsStream(seq)
                    if (texture != null) {
                        val img = ImageIO.read(texture)
                        val fontId = emote.font
                        val font = if (fonts.containsKey(fontId)) {
                            fonts[fontId]!!
                        } else {
                            val f = gen.createFont(fontId, img.width)
                            fonts[fontId] = f
                            f
                        }
                        font.addSprite(img, emote.character)
                    }
                }
            }
        } finally {
            for (font in fonts.values) {
                font.close()
            }
        }
    }
}