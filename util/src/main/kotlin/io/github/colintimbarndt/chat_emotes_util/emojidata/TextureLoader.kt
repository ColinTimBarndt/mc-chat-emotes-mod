package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.WebHelper
import io.github.colintimbarndt.chat_emotes_util.serial.BaseEmojiData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.File
import java.net.URI
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.streams.asSequence

typealias SourceMapper = (Int, Boolean) -> URI

abstract class TextureLoader {
    abstract val sizes: IntArray
    abstract val defaultSize: Int

    abstract class LoadedTextures : Closeable, AutoCloseable {
        abstract operator fun get(data: BaseEmojiData): ImageView?
    }

    data class ImageView(val image: Image, val x: Int, val y: Int, val size: Int)

    abstract suspend fun load(size: Int, clean: Boolean): LoadedTextures

    class AtlasTextureLoader internal constructor(
        private val atlasSource: SourceMapper,
        override val sizes: IntArray,
        override val defaultSize: Int,
        private val padding: Int,
    ) : TextureLoader() {
        override suspend fun load(size: Int, clean: Boolean): LoadedAtlasTextures {
            val image = withContext(Dispatchers.IO) {
                ImageIO.read(atlasSource(size, clean).toURL())
            }
            return LoadedAtlasTextures(image, size)
        }

        inner class LoadedAtlasTextures(val image: BufferedImage, val size: Int) : LoadedTextures() {
            operator fun get(sheetX: Int, sheetY: Int): ImageView {
                // See https://github.com/iamcal/emoji-data#understanding-the-spritesheets
                val paddedSize = size + 2 * padding
                val x = sheetX * paddedSize + padding
                val y = sheetY * paddedSize + padding
                return ImageView(image, x, y, size)
            }

            override fun get(data: BaseEmojiData) = this[data.sheetX.toInt(), data.sheetY.toInt()]

            override fun close() {
                image.flush()
            }
        }

        companion object {
            /**
             * See [Emoji Data README](https://github.com/iamcal/emoji-data#understanding-the-spritesheets)
             */
            const val EMOJI_DATA_PADDING = 1
        }
    }

    class ZipArchiveTextureLoader internal constructor(
        private val atlasSource: SourceMapper,
        override val sizes: IntArray,
        override val defaultSize: Int,
        private val keyMapper: (BaseEmojiData) -> String,
    ) : TextureLoader() {
        override suspend fun load(size: Int, clean: Boolean): LoadedTextures {
            val zipFile = withContext(Dispatchers.IO) {
                val file = cache[size to clean] ?: run {
                    // Download
                    val uri = atlasSource(size, clean)
                    LOGGER.info("Downloading texture archive from {}", uri)
                    val sourceZip = WebHelper.getInputStream(uri)
                    val temp = File.createTempFile("ChatEmotesUtil", ".zip")
                    temp.deleteOnExit()
                    sourceZip.body().transferTo(temp.outputStream())
                    cache[size to clean] = temp
                    temp
                }
                ZipFile(file, ZipFile.OPEN_READ)
            }
            return LoadedZipTextures(zipFile, size)
        }

        inner class LoadedZipTextures(private val zipFile: ZipFile, private val size: Int) : LoadedTextures() {
            override operator fun get(data: BaseEmojiData): ImageView? {
                val key = keyMapper(data)
                val entry = zipFile.getEntry(key) ?: return null
                val stream = zipFile.getInputStream(entry)
                val image = ImageIO.read(stream)
                if (image.width != size || image.height != size) {
                    LOGGER.warn("Texture size mismatch: Expected square ${size}px image, got ${image.width}x${image.height}")
                }
                return ImageView(image, 0, 0, size)
            }
            override fun close() {
                zipFile.close()
            }
        }

        companion object {
            private var cache = hashMapOf<Pair<Int, Boolean>, File>()
        }
    }

    companion object {
        private val resEmojiData = intArrayOf(16, 20, 32, 64)
        private val resOpenMoji = intArrayOf(72, 618)
        private val resJoypixels = intArrayOf(32, 64, 128)

        fun atlas(sizes: IntArray, defaultSize: Int, padding: Int = 0, source: SourceMapper) =
            AtlasTextureLoader(source, sizes, defaultSize, padding)

        fun zipArchive(sizes: IntArray, defaultSize: Int, source: SourceMapper, keyMapper: (BaseEmojiData) -> String) =
            ZipArchiveTextureLoader(source, sizes, defaultSize, keyMapper)

        fun unifiedZipArchive(sizes: IntArray, defaultSize: Int, source: SourceMapper) =
            zipArchive(sizes, defaultSize, source) { data ->
                data.unified.codePoints().asSequence()
                    .map { it.toString(16).uppercase() }
                    .joinToString("-")
            }

        fun emojiDataSpritesheet(vendor: String) =
            atlas(resEmojiData, 32, AtlasTextureLoader.EMOJI_DATA_PADDING) { size, clean ->
                WebHelper.GithubFile(
                    "iamcal", "emoji-data", "master",
                    if (clean) "sheets-clean/sheet_${vendor}_${size}_clean.png"
                    else "sheet_${vendor}_${size}.png"
                ).uri
            }

        fun openmoji(variant: String = "color") =
            unifiedZipArchive(resOpenMoji, 72) { size, _ ->
                URI("https://github.com/hfg-gmuend/openmoji/releases/latest/download/openmoji-${size}x${size}-${variant}.zip")
            }
    }
}