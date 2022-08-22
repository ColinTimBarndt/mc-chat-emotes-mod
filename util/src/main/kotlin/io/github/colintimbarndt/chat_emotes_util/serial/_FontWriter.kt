@file:JvmName("FontWriterKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.serial

import io.github.colintimbarndt.chat_emotes_util.emojidata.TextureLoader
import io.github.colintimbarndt.chat_emotes_util.model.CharArrayString
import io.github.colintimbarndt.chat_emotes_util.model.FontMetadata
import javafx.embed.swing.SwingFXUtils
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.IOException
import javax.imageio.ImageIO

data class FontAssetOptions(
    val glyphSize: Int,
    val atlasSize: Int = 8,
    val glyphHeight: Int = 8,
    val glyphAscent: Int = 8,
    val json: Json = Json,
)

class FontTextureWriter(
    internal val packWriter: PackWriter,
    internal val options: FontAssetOptions,
    private val namespace: String,
    private val name: String,
) : Closeable, AutoCloseable {
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
    internal var locked = false
    private var splitProvider = false

    private inline fun ensureOpen() {
        if (closed) throw IOException("Stream closed")
    }

    private inline fun createCanvas(): Canvas {
        val size = (options.glyphSize * options.atlasSize).toDouble()
        return Canvas(size, size)
    }

    private inline fun createProvider() = FontMetadata.BitmapProvider(
        "$namespace:font/$name/$atlasId.png",
        (Array(options.atlasSize) { CharArrayString(options.atlasSize) }),
        options.glyphAscent,
        options.glyphHeight
    )

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

    internal fun addGlyph(char: Char, sprite: TextureLoader.ImageView): FontMetadata.BitmapProvider? {
        ensureOpen()
        val newProvider = if (glyphs == maxGlyphs) {
            splitProvider = false
            flushAtlas()
            newAtlas()
            true
        } else if (splitProvider) {
            splitProvider = false
            provider = createProvider()
            true
        } else false
        val size = options.glyphSize
        val tileX = glyphs % options.atlasSize
        val tileY = glyphs / options.atlasSize
        val x = tileX * size
        val y = tileY * size
        sprite.drawImage(graphics, x.toDouble(), y.toDouble(), size.toDouble())
        provider.chars[tileY][tileX] = char
        glyphs++
        return if (newProvider) provider else null
    }

    internal fun splitProvider() {
        ensureOpen()
        splitProvider = true
    }

    override fun close() {
        if (closed) return
        closed = true
        flushAtlas()
    }
}

class FontGlyphWriter(
    private val textureWriter: FontTextureWriter,
    private val namespace: String,
    private val name: String,
) : Closeable, AutoCloseable {
    private val metadata = FontMetadata()

    private var closed = false
    private var firstGlyph = true

    constructor(
        packWriter: PackWriter, options: FontAssetOptions, namespace: String, name: String
    ) : this(FontTextureWriter(packWriter, options, namespace, name), namespace, name)

    init {
        if (textureWriter.locked) throw IOException("FontTextureWriter is used by another FontGlyphWriter")
        textureWriter.locked = true
    }

    fun addGlyph(char: Char, sprite: TextureLoader.ImageView) {
        if (firstGlyph) {
            firstGlyph = false
            textureWriter.splitProvider()
        }
        textureWriter.addGlyph(char, sprite)?.let(metadata.providers::add)
    }

    override fun close() {
        if (closed) return
        closed = true
        textureWriter.locked = false
        textureWriter.packWriter.addJsonFile("assets/$namespace/font/$name.json", metadata, textureWriter.options.json)
    }
}

inline fun PackWriter.addFont(namespace: String, name: String, options: FontAssetOptions): FontGlyphWriter =
    FontGlyphWriter(this, options, namespace, name)

inline fun PackWriter.addFont(
    namespace: String, name: String, options: FontAssetOptions, block: FontGlyphWriter.() -> Unit
) = addFont(namespace, name, options).use(block)

inline fun PackWriter.addFonts(
    namespace: String, name: String, options: FontAssetOptions
) = FontTextureWriter(this, options, namespace, name)

inline fun PackWriter.addFonts(
    namespace: String, name: String, options: FontAssetOptions, block: FontTextureWriter.() -> Unit
) = addFonts(namespace, name, options).use(block)

inline fun FontTextureWriter.addFont(namespace: String, name: String): FontGlyphWriter =
    FontGlyphWriter(this, namespace, name)

inline fun FontTextureWriter.addFont(namespace: String, name: String, block: FontGlyphWriter.() -> Unit) =
    addFont(namespace, name).use(block)

