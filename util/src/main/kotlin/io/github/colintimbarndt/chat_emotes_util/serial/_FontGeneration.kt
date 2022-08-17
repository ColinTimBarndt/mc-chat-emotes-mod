@file:JvmName("PackGenerationKt")
@file:Suppress("NOTHING_TO_INLINE")
@file:JvmMultifileClass

package io.github.colintimbarndt.chat_emotes_util.serial

import io.github.colintimbarndt.chat_emotes_util.emojidata.TextureLoader
import javafx.embed.swing.SwingFXUtils
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.IOException
import javax.imageio.ImageIO

data class FontAssetOptions(
    var glyphSize: Int = 0,
    var atlasSize: Int = 8,
    var glyphHeight: Int = 8,
    var glyphAscent: Int = 8,
)

class FontWriter(
    private val packWriter: PackWriter,
    private val options: FontAssetOptions,
    private val namespace: String,
    val name: String,
) : Closeable, AutoCloseable {
    private val metadata = FontMetadata()
    private var atlasId = -1
    private val canvas = createCanvas()
    private val graphics = canvas.graphicsContext2D
    private lateinit var provider: FontMetadata.BitmapProvider
    private val maxGlyphs = options.atlasSize.let { it * it }
    private var glyphs = maxGlyphs
    private var atlasBuffer: BufferedImage? = null
    private val snapshotParameters = SnapshotParameters().apply {
        fill = Color.TRANSPARENT
    }

    private var closed = false

    private inline fun ensureOpen() {
        if (closed) throw IOException("Stream closed")
    }

    private inline fun createCanvas(): Canvas {
        val size = (options.glyphSize * options.atlasSize).toDouble()
        return Canvas(size, size)
    }

    private inline fun createProvider() =
        FontMetadata.BitmapProvider(
            "$namespace:font/$name/$atlasId.png", (Array(options.atlasSize) { CharArrayString(options.atlasSize) }),
            options.glyphAscent, options.glyphHeight
        ).also(metadata.providers::add)

    private fun flushAtlas() {
        if (atlasId < 0) return
        packWriter.addFile("assets/$namespace/textures/font/$name/$atlasId.png") { file ->
            val image = WritableImage(canvas.width.toInt(), canvas.height.toInt())
            canvas.snapshot(snapshotParameters, image)
            atlasBuffer = SwingFXUtils.fromFXImage(image, atlasBuffer)!!
            ImageIO.write(atlasBuffer, "png", file)
        }
    }

    private inline fun newAtlas() {
        atlasId++
        graphics.clearRect(.0, .0, canvas.width, canvas.height)
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
        sprite.drawImage(graphics, x.toDouble(), y.toDouble(), size.toDouble())
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

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonClassDiscriminator("type")
    internal sealed class Provider

    @Serializable
    @SerialName("minecraft:bitmap")
    internal data class BitmapProvider(
        val file: String,
        val chars: Array<CharArrayString>,
        val ascent: Int = 8,
        val height: Int = 8,
    ) : Provider() {

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
