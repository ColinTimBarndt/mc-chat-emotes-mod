package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.emojidata.*
import io.github.colintimbarndt.chat_emotes_util.serial.FontAssetOptions
import io.github.colintimbarndt.chat_emotes_util.serial.PackFormat
import io.github.colintimbarndt.chat_emotes_util.serial.ZipPackWriter
import io.github.colintimbarndt.chat_emotes_util.serial.addMetadata
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import javafx.util.StringConverter
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.CountDownLatch

class EmojiAssetsTabController {
    @FXML
    lateinit var exportButton: Button

    @FXML
    lateinit var gameVersionChoice: ChoiceBox<PackFormat>

    @FXML
    lateinit var textureSource: ChoiceBox<TextureLoader>

    @FXML
    lateinit var variantChoice: ChoiceBox<String>

    @FXML
    lateinit var resolutionChoice: ChoiceBox<Int>

    @FXML
    lateinit var atlasNamespace: TextField

    @FXML
    lateinit var atlasName: TextField

    @FXML
    lateinit var fontNameType: ChoiceBox<FontNamingFactory>

    @FXML
    lateinit var fontNamespace: TextField

    @FXML
    lateinit var fontName: TextField

    @FXML
    lateinit var prettyCheck: CheckBox

    @FXML
    fun initialize() {
        gameVersionChoice.items.setAll(PackFormat.values)
        gameVersionChoice.value = PackFormat.values.last()

        resolutionChoice.converter = object : StringConverter<Int>() {
            override fun toString(num: Int?) = if (num == null) "" else "x$num"
            override fun fromString(string: String?) = string?.substring(1)?.toInt()
        }

        textureSource.items.setAll(TextureLoader.EMOJI_TEXTURES)
        textureSource.setOnAction {
            val source = textureSource.value!!
            run {
                // Update resolution settings
                val prevRes = resolutionChoice.value
                resolutionChoice.items.setAll(*source.sizes.toTypedArray())
                // If possible, don't change the resolution setting
                resolutionChoice.value = if (
                    prevRes == null
                    || source.sizes.binarySearch(prevRes) < 0
                ) source.defaultSize
                else prevRes
            }
            run {
                // Update variant settings
                val prevVar = variantChoice.value
                variantChoice.items.setAll(source.variants.values)
                // If possible, don't change the variant setting
                variantChoice.value = if (
                    prevVar == null
                    || prevVar !in source.variants.values
                ) source.defaultVariant
                else prevVar
            }
        }
        textureSource.value = TextureLoader.EMOJI_TEXTURES.last()

        fontNameController(fontNameType, fontName)

        exportButton.isDisable = false
    }

    @OptIn(DelicateCoroutinesApi::class)
    @FXML
    fun export() {
        val choose = FileChooser().apply {
            title = "Save Emoji Resourcepack"
            extensionFilters.setAll(zipExtensionFilter)
            initialFileName = "emoji.zip"
            selectedExtensionFilter = zipExtensionFilter
        }
        exportButton.isDisable = true
        val file = choose.showSaveDialog(App.INSTANCE.stage)
        if (file == null) {
            exportButton.isDisable = false
            return;
        }
        GlobalScope.launch {
            LOGGER.info("Writing resource pack to {}", file.absolutePath)
            val sourceSize = resolutionChoice.value!!
            val variant = variantChoice.value!!
            LOGGER.info("Variant: '{}'", variant)
            val data = streamIncludedEmojiData()
                .flatEmojiData()
                .expandToEmoteData(
                    EmojiAliasSource.None.load(),
                    fontNamespace.text,
                    fontNameType.value.create(fontName.text)
                )
            val textures = withContext(Dispatchers.IO) {
                textureSource.value!!.load(sourceSize, variant)
            }
            val json = if (prettyCheck.isSelected) Json { prettyPrint = true } else Json
            val glyphOptions = FontAssetOptions(glyphSize = sourceSize, json = json)
            ZipPackWriter.of(file).use { packWriter ->
                packWriter.addMetadata(json) {
                    pack.format = gameVersionChoice.value.resourceFormat
                    pack.description = buildJsonObject {
                        put("text", "Emoji Chat Emotes")
                        put("color", "yellow")
                    }
                }
                val lock = CountDownLatch(1)
                Platform.runLater {
                    try {
                        writeFonts(
                            packWriter,
                            data,
                            textures,
                            glyphOptions,
                            atlasNamespace.text,
                            atlasName.text,
                        )
                    } finally {
                        lock.countDown()
                    }
                }
                withContext(Dispatchers.IO) { lock.await() }
            }
            LOGGER.info("Done writing resource pack")
            exportButton.isDisable = false
        }
    }
}