package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.emojidata.*
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EmojiDataTab : BorderPane() {
    init {
        loadFXML<Node>("/scenes/EmojiDataTab.fxml", this)
    }

    @FXML
    lateinit var emojiSourcesForm: EmojiSourcesForm

    @FXML
    lateinit var exportButton: Button

    @FXML
    lateinit var fontNameType: ChoiceBox<FontNamingFactory>

    @FXML
    lateinit var fontNamespace: TextField

    @FXML
    lateinit var fontName: TextField

    @FXML
    lateinit var prettyCheck: CheckBox

    @FXML
    @Suppress("UNUSED")
    fun initialize() {
        fontNameController(fontNameType, fontName)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @FXML
    @Suppress("UNUSED")
    fun export() {
        val choose = FileChooser().apply {
            title = "Save Emoji Data"
            extensionFilters.setAll(jsonExtensionFilter)
            initialFileName = "emoji.json"
            selectedExtensionFilter = jsonExtensionFilter
        }
        exportButton.isDisable = true
        val file = choose.showSaveDialog(App.INSTANCE.stage)
        if (file == null) {
            exportButton.isDisable = false
            return
        }
        val job = GlobalScope.launch(Dispatchers.IO) {
            LOGGER.info("Writing emoji data to {}", file.absolutePath)
            val data = EmojiDataProvider.loadSequence()
                .flatEmojiData()
                .expandToEmoteData(
                    emojiSourcesForm.loadSources(),
                    fontNamespace.text,
                    fontNameType.value.create(fontName.text)
                )
                .flatMap { it.second }
                .map(ExpandedEmoteData::emote)
            file.outputStream().use { stream ->
                writeChatEmoteData(data, stream, prettyCheck.isSelected)
            }
        }
        job.invokeOnCompletion {
            LOGGER.info("Done writing emoji data")
            exportButton.isDisable = false
        }
    }
}