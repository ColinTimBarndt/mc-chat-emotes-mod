@file:JvmName("TextureLoaderKt")
@file:OptIn(ExperimentalSerializationApi::class)

package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.model.BaseEmojiData
import io.github.colintimbarndt.chat_emotes_util.model.UnicodeSequence
import io.github.colintimbarndt.chat_emotes_util.streamAsset
import io.github.colintimbarndt.chat_emotes_util.web.FileSourceTemplate
import io.github.colintimbarndt.chat_emotes_util.web.WebHelper.STANDARD_CACHE_TIME
import io.github.colintimbarndt.chat_emotes_util.web.getInputStreamSync
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.decodeFromStream
import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@JvmInline
@Serializable
value class TextureSource(
    private val template: FileSourceTemplate
) {
    fun map(resolution: Int, data: Map<String, String>) = template.resolveTemplate {
        when (it) {
            "size" -> resolution.toString()
            in data -> data[it]!!
            else -> ""
        }
    }
}

@Serializable
@JsonClassDiscriminator("type")
sealed class TextureLoader : Labeled {
    abstract val sizes: IntArray
    abstract val defaultSize: Int
    abstract val variants: TextureVariants
    abstract val defaultVariant: String

    abstract suspend fun load(size: Int, variant: String): LoadedTextures

    protected operator fun TextureSource.get(size: Int, variant: String) =
        map(size, variants.dataFor(variant))

    abstract class LoadedTextures : Closeable, AutoCloseable {
        abstract operator fun get(data: BaseEmojiData): ImageView?
    }

    data class ImageView(val image: Image, val x: Double, val y: Double, val size: Double) {
        @Suppress("NOTHING_TO_INLINE")
        inline fun drawImage(graphics: GraphicsContext, dx: Double, dy: Double, dSize: Double) {
            graphics.drawImage(
                image, x, y, size, size, dx, dy, dSize, dSize
            )
        }
    }

    @Serializable
    sealed class AbstractSpritesheetTextureLoader : TextureLoader() {
        protected abstract val padding: Int
        protected abstract val textures: TextureSource
        override suspend fun load(size: Int, variant: String): LoadedAtlasTextures {
            val image = withContext(Dispatchers.IO) {
                Image(textures[size, variant].getInputStreamSync(STANDARD_CACHE_TIME).result)
            }
            if (image.exception != null) throw image.exception
            return LoadedAtlasTextures(image, size.toDouble())
        }

        inner class LoadedAtlasTextures(val image: Image, private val size: Double) : LoadedTextures() {
            operator fun get(sheetX: Int, sheetY: Int): ImageView {
                // See https://github.com/iamcal/emoji-data#understanding-the-spritesheets
                val paddedSize = size + 2 * padding
                val x = sheetX * paddedSize + padding
                val y = sheetY * paddedSize + padding
                return ImageView(image, x, y, size)
            }

            override fun get(data: BaseEmojiData) = this[data.sheetX.toInt(), data.sheetY.toInt()]

            override fun close() {}
        }
    }

    @Serializable
    @SerialName("spritesheet")
    @Suppress("UNUSED")
    class SpritesheetTextureLoader(
        @SerialName("name")
        override val label: String,
        override val padding: Int = 0,
        override val textures: TextureSource,
        override val sizes: IntArray,
        @SerialName("default_size") override val defaultSize: Int,
        override val variants: TextureVariants,
        @SerialName("default_variant") override val defaultVariant: String,
    ) : AbstractSpritesheetTextureLoader()

    @Serializable
    sealed class ZipArchiveTextureLoader<K> : TextureLoader() {
        protected abstract val textures: TextureSource
        abstract fun getKey(data: BaseEmojiData): K
        abstract fun buildIndex(file: ZipFile): Map<K, ZipEntry>

        @Transient
        private val cache = hashMapOf<Pair<Int, String>, File>()

        override suspend fun load(size: Int, variant: String): LoadedTextures {
            val zipFile = withContext(Dispatchers.IO) {
                val file = cache[size to variant] ?: run {
                    // Download
                    val src = textures[size, variant]
                    val temp = File.createTempFile("ChatEmotesUtil", ".zip")
                    temp.deleteOnExit()
                    val sourceZip = src.getInputStreamSync(STANDARD_CACHE_TIME)
                    sourceZip.result.transferTo(temp.outputStream())
                    cache[size to variant] = temp
                    temp
                }
                ZipFile(file, ZipFile.OPEN_READ)
            }
            return LoadedZipTextures(zipFile, size.toDouble())
        }

        inner class LoadedZipTextures(private val zipFile: ZipFile, private val size: Double) : LoadedTextures() {
            private val index = buildIndex(zipFile)
            override operator fun get(data: BaseEmojiData): ImageView? {
                val entry = index[getKey(data)] ?: return null
                val stream = zipFile.getInputStream(entry)
                val image = Image(stream)
                if (image.exception != null) throw image.exception
                if (image.width != size || image.height != size) {
                    LOGGER.warn("Texture size mismatch: Expected square ${size}px image, got ${image.width}x${image.height}")
                }
                return ImageView(image, .0, .0, size)
            }

            override fun close() {
                zipFile.close()
            }
        }
    }

    @Serializable
    @SerialName("unified-zip")
    @Suppress("UNUSED")
    class UnifiedZipArchiveTextureLoader internal constructor(
        @SerialName("name")
        override val label: String,
        override val textures: TextureSource,
        override val sizes: IntArray,
        @SerialName("default_size") override val defaultSize: Int,
        override val variants: TextureVariants,
        @SerialName("default_variant") override val defaultVariant: String,
    ) : ZipArchiveTextureLoader<UnicodeSequence>() {
        override fun getKey(data: BaseEmojiData) = data.unified

        override fun buildIndex(file: ZipFile): Map<UnicodeSequence, ZipEntry> {
            val entries = file.entries()
            val map = hashMapOf<UnicodeSequence, ZipEntry>()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                var name = entry.name
                if (name.endsWith(".png", ignoreCase = true)) {
                    name = name.substring(0 until name.length - 4)
                    runCatching {
                        UnicodeSequence.parse(name)
                    }.onSuccess { key ->
                        map += key to entry
                    }
                }
            }
            return map
        }
    }

    companion object {
        val EMOJI_TEXTURES: List<TextureLoader> by lazy {
            Json.decodeFromStream(streamAsset("/assets/emojiTextures.json")!!)
        }
    }
}