package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.App
import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.emojidata.*
import io.github.colintimbarndt.chat_emotes_util.serial.FontAssetOptions
import io.github.colintimbarndt.chat_emotes_util.serial.PackFormat
import io.github.colintimbarndt.chat_emotes_util.serial.PackWriter
import io.github.colintimbarndt.chat_emotes_util.serial.addMetadata
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.*
import javafx.stage.FileChooser
import javafx.util.StringConverter
import kotlinx.coroutines.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.concurrent.CountDownLatch

class EmojiDataController {
    companion object : TabEntry {
        override val title = "Emoji"

        override fun createBodyNode(): Node =
            FXMLLoader.load(App::class.java.getResource("/scenes/EmojiData.fxml"))

        private val jsonExtensionFilter = FileChooser.ExtensionFilter("JSON Files", "*.json")
        private val zipExtensionFilter = FileChooser.ExtensionFilter("ZIP Files", "*.zip")
    }

    /* DATA */

    @FXML
    lateinit var exportDataButton: Button

    @FXML
    lateinit var dataAliasesCombo: ListView<EmojiAliasSource>

    @FXML
    lateinit var prettyDataCheck: CheckBox

    /* ASSETS */

    @FXML
    lateinit var exportAssetsButton: Button

    @FXML
    lateinit var assetGameVersionChoice: ChoiceBox<PackFormat>

    @FXML
    lateinit var assetTextureSource: ChoiceBox<EmojiTextureSource>

    @FXML
    lateinit var cleanAssetsCheck: CheckBox

    @FXML
    lateinit var assetResolutionChoice: ChoiceBox<Int>

    @FXML
    lateinit var prettyAssetsCheck: CheckBox

    @OptIn(DelicateCoroutinesApi::class)
    @FXML
    fun initialize() {
        //GlobalScope.launch {
        //    val latest = try {
        //        EmojiDataImport.latestEmojiFile.getCommitHash()
        //    } catch (e: Exception) {
        //        LOGGER.error("Unable to load latest emoji-data commit hash", e)
        //        return@launch
        //    }
        //    if (latest == EmojiDataImport.includedEmojiCommitHash) {
        //        LOGGER.info("Included emoji-data is up-to-date")
        //    } else {
        //        LOGGER.warn("Included emoji-data is outdated")
        //    }
        //}

        dataAliasesCombo.items.setAll(*EmojiAliasSource.values())
        dataAliasesCombo.selectionModel.run {
            selectionMode = SelectionMode.SINGLE
            select(EmojiAliasSource.EmojiData)
            selectedItems.addListener(ListChangeListener {
                exportDataButton.isDisable = it.list.isEmpty()
            })
        }

        exportDataButton.isDisable = false

        assetGameVersionChoice.items.setAll(PackFormat.values)
        assetGameVersionChoice.value = PackFormat.values.last()

        assetResolutionChoice.converter = object : StringConverter<Int>() {
            override fun toString(num: Int?) = if (num == null) "" else "x$num"
            override fun fromString(string: String?) = string?.substring(1)?.toInt()
        }

        assetTextureSource.items.setAll(*EmojiTextureSource.values())
        assetTextureSource.setOnAction {
            val source = assetTextureSource.value!!
            val prevValue = assetResolutionChoice.value
            assetResolutionChoice.items.setAll(*source.loader.sizes.toTypedArray())
            // If possible, don't change the resolution setting
            assetResolutionChoice.value = if (
                prevValue == null
                || source.loader.sizes.binarySearch(prevValue) < 0
            ) source.loader.defaultSize
            else prevValue
        }
        assetTextureSource.value = EmojiTextureSource.default
    }

    @OptIn(DelicateCoroutinesApi::class)
    @FXML
    fun exportData() {
        val choose = FileChooser().apply {
            title = "Save Emoji Data"
            extensionFilters.setAll(jsonExtensionFilter)
            initialFileName = "emoji.json"
            selectedExtensionFilter = jsonExtensionFilter
        }
        exportDataButton.isDisable = true
        val file: File = choose.showSaveDialog(App.INSTANCE.stage)
        GlobalScope.launch(Dispatchers.IO) {
            LOGGER.info("Writing emoji data to {}", file.absolutePath)
            val data = streamIncludedEmojiData()
                .flatEmojiData()
                .expandToEmoteData(dataAliasesCombo.selectionModel.selectedItem!!.load())
                .map(ExpandedEmoteData::emote)
            file.outputStream().use { stream ->
                writeChatEmoteData(data, stream, prettyDataCheck.isSelected)
            }
            LOGGER.info("Done writing emoji data")
            exportDataButton.isDisable = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @FXML
    fun exportAssets() {
        val choose = FileChooser().apply {
            title = "Save Emoji Resourcepack"
            extensionFilters.setAll(zipExtensionFilter)
            initialFileName = "emoji.zip"
            selectedExtensionFilter = zipExtensionFilter
        }
        exportAssetsButton.isDisable = true
        val file: File = choose.showSaveDialog(App.INSTANCE.stage)
        GlobalScope.launch {
            LOGGER.info("Writing resource pack to {}", file.absolutePath)
            val sourceSize = assetResolutionChoice.value!!
            val cleanLicense = cleanAssetsCheck.isSelected
            val data = streamIncludedEmojiData()
                .flatEmojiData()
                .expandToEmoteData(EmojiAliasSource.None.load())
            val textures = withContext(Dispatchers.IO) {
                assetTextureSource.value!!.loader.load(sourceSize, cleanLicense)
            }
            val glyphOptions = FontAssetOptions(glyphSize = sourceSize)
            PackWriter.of(file).use { packWriter ->
                packWriter.addMetadata {
                    pack.format = assetGameVersionChoice.value.resourceFormat
                    pack.description = buildJsonObject {
                        put("text", "Emoji Chat Emotes")
                        put("color", "yellow")
                    }
                }
                val lock = CountDownLatch(1)
                Platform.runLater {
                    var fontId = 0
                    try {
                        writeFonts(packWriter, data, textures, glyphOptions, "chat_emotes") {
                            "emoji_" + fontId++
                        }
                    } finally {
                        lock.countDown()
                    }
                }
                withContext(Dispatchers.IO) { lock.await() }
            }
            LOGGER.info("Done writing resource pack")
            exportAssetsButton.isDisable = false
        }
    }
}