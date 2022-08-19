package io.github.colintimbarndt.chat_emotes_util.fx

import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*

class EmojiTabController {
    companion object : TabEntry {
        override val title = "Emoji"
        override fun createBodyNode(): Node = loadFXML("/scenes/EmojiTab.fxml")
    }

    @FXML
    lateinit var dataTab: Tab

    @FXML
    lateinit var assetsTab: Tab

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

        lazyLoadTab(dataTab) {
            content = loadFXML("/scenes/EmojiDataTab.fxml")
        }
        lazyLoadTab(assetsTab) {
            content = loadFXML("/scenes/EmojiAssetsTab.fxml")
        }
    }
}