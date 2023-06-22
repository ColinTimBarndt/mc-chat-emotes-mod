package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.emojidata.*
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.ChoiceBox
import javafx.scene.layout.GridPane

class EmojiSourcesForm : GridPane() {
    @FXML
    lateinit var nameChoice: ChoiceBox<EmojiNameSource> private set

    @FXML
    lateinit var categoryChoice: ChoiceBox<EmojiCategorySource> private set

    @FXML
    lateinit var aliasesChoice: ChoiceBox<EmojiAliasSource> private set

    @FXML
    lateinit var emoticonsChoice: ChoiceBox<EmojiEmoticonSource> private set

    init {
        loadFXML<Node>("/scenes/EmojiSourcesForm.fxml", this)
    }

    @FXML
    @Suppress("UNUSED")
    fun initialize() {
        nameChoice.setItemsEnum(EmojiNameSource)
        categoryChoice.setItemsEnum(EmojiCategorySource)
        aliasesChoice.setItemsEnum(EmojiAliasSource)
        emoticonsChoice.setItemsEnum(EmojiEmoticonSource)
    }

    suspend inline fun loadSources() = ComposedEmojiSourceMapper.load(
        nameSource = nameChoice.value,
        categorySource = categoryChoice.value,
        aliasSource = aliasesChoice.value,
        emoticonSource = emoticonsChoice.value,
    )

    var disableName: Boolean
        get() = nameChoice.isDisable
        set(value) { nameChoice.isDisable = value }

    var disableCategory: Boolean
        get() = categoryChoice.isDisable
        set(value) { categoryChoice.isDisable = value }

    var disableAliases: Boolean
        get() = aliasesChoice.isDisable
        set(value) { aliasesChoice.isDisable = value }

    var disableEmoticons: Boolean
        get() = emoticonsChoice.isDisable
        set(value) { emoticonsChoice.isDisable = value }
}