package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.emojidata.*
import io.github.colintimbarndt.chat_emotes_util.serial.*
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.OutputStream

class EmojiDataTab : BorderPane() {
    init {
        loadFXML<Node>("/scenes/EmojiDataTab.fxml", this)
    }

    @FXML
    lateinit var gameVersionChoice: GameVersionChoiceBox

    @FXML
    lateinit var emojiSourcesForm: EmojiSourcesForm

    @FXML
    lateinit var exportButton: MenuButton

    @FXML
    lateinit var fontNameType: ChoiceBox<FontNamingFactory>

    @FXML
    lateinit var fontNamespace: TextField

    @FXML
    lateinit var fontName: TextField

    @FXML
    lateinit var prettyCheck: CheckBox

    @FXML
    lateinit var packMetaCheck: CheckBox

    @FXML
    @Suppress("UNUSED")
    fun initialize() {
        fontNameController(fontNameType, fontName)

        for (writerFactory in PackWriter.values) {
            val menuItem = MenuItem(writerFactory.label)
            menuItem.setOnAction {
                exportDataPack(writerFactory)
            }
            exportButton.items.add(menuItem)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @FXML
    @Suppress("UNUSED")
    fun export() {
        exportButton.isDisable = true
        val file = FileType.Json.showSaveDialog("Save Emoji Data", "emoji.json")
        if (file == null) {
            exportButton.isDisable = false
            return
        }
        val job = GlobalScope.launch(Dispatchers.IO) {
            LOGGER.info("Writing emoji data to {}", file.absolutePath)
            writeEmojiData(file.outputStream())
        }
        job.invokeOnCompletion { error ->
            if (error != null) {
                LOGGER.error("Error writing emoji data", error)
            } else {
                LOGGER.info("Done writing emoji data")
            }
            exportButton.isDisable = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun exportDataPack(writerFactory: PackWriterFactory) {
        exportButton.isDisable = true
        val file = FileType.Json.showSaveDialog("Save Emoji Data Pack", "Emoji" + writerFactory.fileType.extension)
        if (file == null) {
            exportButton.isDisable = false
            return
        }
        val job = GlobalScope.launch(Dispatchers.IO) {
            LOGGER.info("Writing emoji data pack to {}", file.absolutePath)
            val writer = writerFactory.create(file)
            writer.addFile("data/${fontNamespace.text}/emote/emoji.json") {
                writeEmojiData(it)
            }
            if (packMetaCheck.isSelected) {
                writer.addMetadata {
                    pack.format = gameVersionChoice.value.resourceFormat
                    pack.description = buildJsonObject {
                        put("text", "Emoji Chat Emotes")
                        put("color", "yellow")
                    }
                }
            }
        }
        job.invokeOnCompletion { error ->
            if (error != null) {
                LOGGER.error("Error writing emoji data", error)
            } else {
                LOGGER.info("Done writing emoji data")
            }
            exportButton.isDisable = false
        }
    }

    private suspend fun writeEmojiData(stream: OutputStream) {
        val data = EmojiDataProvider.loadSequence().flatEmojiData().expandToEmoteData(
            emojiSourcesForm.loadSources(),
            fontNamespace.text,
            fontNameType.value.create(fontName.text)
        ).flatMap { it.second }.map(ExpandedEmoteData::emote)

        stream.use {
            writeChatEmoteData(data, it, prettyCheck.isSelected)
        }
    }
}