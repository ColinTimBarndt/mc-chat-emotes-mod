package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.emojidata.*
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EmojiDataTabController {

    @FXML
    lateinit var exportButton: Button

    @FXML
    lateinit var aliasesCombo: ChoiceBox<EmojiAliasSource>

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
        aliasesCombo.items.setAll(*EmojiAliasSource.values())
        aliasesCombo.value = EmojiAliasSource.EmojiData

        fontNameController(fontNameType, fontName)

        exportButton.isDisable = false
    }

    @OptIn(DelicateCoroutinesApi::class)
    @FXML
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
            return;
        }
        GlobalScope.launch(Dispatchers.IO) {
            LOGGER.info("Writing emoji data to {}", file.absolutePath)
            val data = streamIncludedEmojiData()
                .flatEmojiData()
                .expandToEmoteData(
                    aliasesCombo.value.load(),
                    fontNamespace.text,
                    fontNameType.value.create(fontName.text)
                )
                .flatMap { it.second }
                .map(ExpandedEmoteData::emote)
            file.outputStream().use { stream ->
                writeChatEmoteData(data, stream, prettyCheck.isSelected)
            }
            LOGGER.info("Done writing emoji data")
            exportButton.isDisable = false
        }
    }
}