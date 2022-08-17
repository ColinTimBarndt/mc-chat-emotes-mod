package io.github.colintimbarndt.chat_emotes_util.emojidata

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.WebHelper
import io.github.colintimbarndt.chat_emotes_util.serial.BaseEmojiData
import io.github.colintimbarndt.chat_emotes_util.serial.UnicodeSequence
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.streams.asSequence

fun interface SourceMapper {
    fun map(resolution: Int, clean: Boolean): WebHelper.FileSource

}

inline operator fun SourceMapper.invoke(resolution: Int, clean: Boolean) = map(resolution, clean).uri

abstract class TextureLoader {
    abstract val sizes: IntArray
    abstract val defaultSize: Int

    abstract class LoadedTextures : Closeable, AutoCloseable {
        abstract operator fun get(data: BaseEmojiData): ImageView?
    }

    data class ImageView(val image: Image, val x: Double, val y: Double, val size: Double) {
        @Suppress("NOTHING_TO_INLINE")
        inline fun drawImage(graphics: GraphicsContext, dx: Double, dy: Double, dSize: Double) {
            if (image.isBackgroundLoading) {
                LOGGER.warn("Loading image will be ignored")
            }
            graphics.drawImage(
                image,
                x, y, size, size,
                dx, dy, dSize, dSize
            )
        }
    }

    abstract suspend fun load(size: Int, clean: Boolean): LoadedTextures

    class AtlasTextureLoader internal constructor(
        private val atlasSource: SourceMapper,
        override val sizes: IntArray,
        override val defaultSize: Int,
        private val padding: Int,
    ) : TextureLoader() {
        override suspend fun load(size: Int, clean: Boolean): LoadedAtlasTextures {
            val image = withContext(Dispatchers.IO) {
                Image(WebHelper.getInputStream(atlasSource(size, clean)).body())
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

        companion object {
            /**
             * See [Emoji Data README](https://github.com/iamcal/emoji-data#understanding-the-spritesheets)
             */
            const val EMOJI_DATA_PADDING = 1
        }
    }

    sealed class ZipArchiveTextureLoader<K>(
        private val atlasSource: SourceMapper,
        override val sizes: IntArray,
        override val defaultSize: Int,
    ) : TextureLoader() {
        abstract fun getKey(data: BaseEmojiData): K
        abstract fun buildIndex(file: ZipFile): Map<K, ZipEntry>
        private val cache = hashMapOf<Pair<Int, Boolean>, File>()
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

    class UnifiedZipArchiveTextureLoader internal constructor(atlasSource: SourceMapper, sizes: IntArray, defaultSize: Int) :
        ZipArchiveTextureLoader<UnicodeSequence>(
            atlasSource, sizes, defaultSize
        ) {
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
        private val resEmojiData = intArrayOf(16, 20, 32, 64)
        private val resOpenMoji = intArrayOf(72, 618)
        private val resJoypixels = intArrayOf(32, 64, 128)

        fun atlas(sizes: IntArray, defaultSize: Int, padding: Int = 0, source: SourceMapper) =
            AtlasTextureLoader(source, sizes, defaultSize, padding)

        fun unifiedZipArchive(sizes: IntArray, defaultSize: Int, source: SourceMapper) =
            UnifiedZipArchiveTextureLoader(source, sizes, defaultSize)

        fun emojiDataSpritesheet(vendor: String) =
            atlas(resEmojiData, 32, AtlasTextureLoader.EMOJI_DATA_PADDING) { size, clean ->
                WebHelper.GithubFile(
                    "iamcal", "emoji-data", "master",
                    if (clean) "sheets-clean/sheet_${vendor}_${size}_clean.png"
                    else "sheet_${vendor}_${size}.png"
                )
            }

        fun openmoji(variant: String = "color") =
            unifiedZipArchive(resOpenMoji, 72) { size, _ ->
                WebHelper.GithubRelease(
                    "hfg-gmuend",
                    "openmoji",
                    "latest",
                    "openmoji-${size}x${size}-${variant}.zip"
                )
            }
    }
}