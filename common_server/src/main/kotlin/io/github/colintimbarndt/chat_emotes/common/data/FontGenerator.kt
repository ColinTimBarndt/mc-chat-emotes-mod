package io.github.colintimbarndt.chat_emotes.common.data

import com.google.gson.stream.JsonWriter
import io.github.colintimbarndt.chat_emotes.common.util.PackWriter
import net.minecraft.resources.ResourceLocation
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.imageio.ImageIO

class FontGenerator(
    private val atlasSprites: Int,
    private val writer: PackWriter
) {
    fun createFont(id: ResourceLocation, textureSize: Int): Font {
        return Font(id, textureSize)
    }

    inner class Font internal constructor(id: ResourceLocation, spriteSize: Int) : Closeable, AutoCloseable {
        private val textures: MutableList<FontTexture> = ArrayList()
        private var currentTexture: FontTexture? = null
        private val id: ResourceLocation
        private val spriteSize: Int
        private var textureIndex = Int.MAX_VALUE
        private var finished = false

        init {
            this.id = ResourceLocation(id.namespace, "font/" + id.path)
            this.spriteSize = spriteSize
        }

        @Throws(IOException::class)
        fun addSprite(sprite: BufferedImage, character: Char) {
            check(!finished) { "Already finished this font" }
            require(
                !(sprite.width != spriteSize
                        || sprite.height != spriteSize)
            ) { "Invalid image size" }
            if (textureIndex >= atlasSprites * atlasSprites) {
                if (currentTexture != null) {
                    currentTexture!!.close()
                }
                val cTex = FontTexture(getTextureLocation(textures.size), spriteSize)
                textures.add(cTex)
                cTex.graphics!!.drawImage(sprite, 0, 0, spriteSize, spriteSize, null)
                cTex.characters[0] = character
                textureIndex = 1
                currentTexture = cTex
            } else {
                val x = textureIndex % atlasSprites * spriteSize
                val y = textureIndex / atlasSprites * spriteSize
                currentTexture!!.graphics!!.drawImage(sprite, x, y, spriteSize, spriteSize, null)
                currentTexture!!.characters[textureIndex] = character
                textureIndex++
            }
        }

        @Throws(IOException::class)
        override fun close() {
            check(!finished) { "Already finished this font" }
            finished = true
            if (currentTexture != null) {
                currentTexture!!.close()
                currentTexture = null
            }
            writer.write(
                ResourceLocation(id.namespace, id.path + ".json")
            ) { stream: OutputStream -> writeFontJson(stream) }
            textures.clear()
        }

        private fun getTextureLocation(i: Int): ResourceLocation {
            val path = id.path
            return ResourceLocation(id.namespace, "textures/" + path + "_" + i + ".png")
        }

        @Throws(IOException::class)
        private fun writeFontJson(stream: OutputStream) {
            JsonWriter(OutputStreamWriter(stream)).use { writer ->
                writer.setIndent("  ")
                writer.beginObject()
                    .name("providers").beginArray()
                for (texture in textures) {
                    writer.beginObject()
                        .name("type").value("bitmap")
                        .name("file").value(texture.textureLocation.toString())
                        .name("ascent").value(8)
                        .name("chars").beginArray()
                    var i = 0
                    while (i < texture.characters.size) {
                        val sb = StringBuilder(2 * atlasSprites)
                        for (j in 0 until atlasSprites) {
                            sb.append(texture.characters[i + j])
                        }
                        writer.value(sb.toString())
                        i += atlasSprites
                    }
                    writer.endArray()
                        .endObject()
                }
                writer.endArray().endObject()
            }
        }
    }

    internal inner class FontTexture internal constructor(location: ResourceLocation, spriteSize: Int) : Closeable,
        AutoCloseable {
        internal val characters: CharArray
        private val location: ResourceLocation
        private var image: BufferedImage?
        internal var graphics: Graphics2D?

        init {
            this.location = location
            characters = CharArray(atlasSprites * atlasSprites)
            val textureSize = spriteSize * atlasSprites
            image = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB)
            graphics = image!!.createGraphics()
        }

        @Throws(IOException::class)
        override fun close() {
            if (graphics != null) {
                graphics!!.dispose()
                graphics = null
                writer.write(location) { ImageIO.write(image, "png", it) }
                image = null
            }
        }

        val textureLocation: ResourceLocation
            get() = ResourceLocation(
                location.namespace,
                location.path.substring("textures/".length)
            )
    }
}