@file:JvmName("PackGenerationKt")
@file:Suppress("NOTHING_TO_INLINE")
@file:JvmMultifileClass

package io.github.colintimbarndt.chat_emotes_util.serial

import io.github.colintimbarndt.chat_emotes_util.emojidata.TextureLoader
import kotlinx.serialization.Serializable
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.IOException
import javax.imageio.ImageIO

data class FontAssetOptions(
    val glyphSize: Int = 0,
    val atlasSize: Int = 8,
    val glyphHeight: Int = 8,
    val glyphAscent: Int = 8,
)

class FontWriter(
    private val packWriter: PackWriter,
    private val options: FontAssetOptions,
    private val namespace: String,
    val name: String,
) : Closeable, AutoCloseable {
    private val metadata = FontMetadata()
    private var atlasId = 0
    private var image = createImage()
    private var graphics = image.graphics
    private var provider = createProvider()
    private val maxGlyphs = options.atlasSize.let { it * it }
    private var glyphs = 0

    private var closed = false

    init {
        metadata.providers
    }

    private inline fun ensureOpen() {
        if (closed) throw IOException("Stream closed")
    }

    private inline fun createImage(): BufferedImage {
        val size = options.glyphSize * options.atlasSize
        return BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR)
    }

    private inline fun createProvider() =
        FontMetadata.BitmapProvider(
            "$namespace:font/$name/$atlasId.png", Array(options.atlasSize) { CharArray(options.atlasSize) },
            options.glyphAscent, options.glyphHeight
        ).also(metadata.providers::add)

    private fun flushAtlas() {
        image.flush()
        graphics.dispose()
        packWriter.addFile("assets/$namespace/textures/font/$name/$atlasId.png") { file ->
            ImageIO.write(image, "png", file)
        }
    }

    private inline fun newAtlas() {
        atlasId++
        image = createImage()
        graphics = image.graphics
        provider = createProvider()
        glyphs = 0
    }

    fun addGlyph(char: Char, sprite: TextureLoader.ImageView) {
        ensureOpen()
        if (glyphs == maxGlyphs) {
            flushAtlas()
            newAtlas()
        }
        val size = options.glyphSize
        val tileX = glyphs % options.atlasSize
        val tileY = glyphs / options.atlasSize
        val x = tileX * size
        val y = tileY * size
        graphics.drawImage(
            sprite.image,
            x, y, size, size,
            sprite.x, sprite.y, sprite.size, sprite.size,
            null
        )
        provider.chars[tileY][tileX] = char
        glyphs++
    }

    override fun close() {
        if (closed) return
        flushAtlas()
        packWriter.addJsonFile("assets/$namespace/fonts/$name.json", metadata)
    }
}

inline fun PackWriter.addFont(namespace: String, name: String, options: FontAssetOptions): FontWriter =
    FontWriter(this, options, namespace, name)

inline fun PackWriter.addFont(namespace: String, name: String, apply: FontAssetOptions.() -> Unit): FontWriter {
    val options = FontAssetOptions()
    options.apply()
    return addFont(namespace, name, options)
}

@Serializable
internal class FontMetadata {
    val providers: ArrayList<Provider> = arrayListOf()

    @Serializable
    internal sealed class Provider {
        abstract val type: String
    }

    @Serializable
    internal data class BitmapProvider(
        val file: String,
        val chars: Array<CharArray>,
        val ascent: Int = 8,
        val height: Int = 8,
    ) : Provider() {
        override val type = "bitmap"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BitmapProvider

            if (file != other.file) return false
            if (ascent != other.ascent) return false
            if (height != other.height) return false
            if (!chars.contentDeepEquals(other.chars)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = file.hashCode()
            result = 31 * result + ascent
            result = 31 * result + height
            result = 31 * result + chars.contentDeepHashCode()
            return result
        }
    }
}
