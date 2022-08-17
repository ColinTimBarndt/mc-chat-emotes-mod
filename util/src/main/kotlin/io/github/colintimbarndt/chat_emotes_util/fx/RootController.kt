package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.App
import javafx.event.Event
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.io.InputStreamReader

class RootController {
    @FXML
    lateinit var tabs: TabPane

    @FXML
    lateinit var aboutText: TextFlow

    @FXML
    fun initialize() {
        for (tab in tabEntries) {
            val tabNode = Tab(tab.title)
            tabs.tabs.add(tabNode)
            lazyLoadTab(tabNode) {
                tabNode.content = tab.createBodyNode()
            }
        }
        val aboutSrc = "/assets/about.txt"
        val about = RootController::class.java.getResourceAsStream(aboutSrc)!!
            .let(::InputStreamReader)
            .use { it.readText() }
        aboutText.children.add(Text(about))
    }

    @FXML
    fun openGithub() {
        App.INSTANCE.hostServices.showDocument("https://github.com/ColinTimBarndt/fabric_chat-emotes#readme")
    }

    companion object {
        val tabEntries: MutableList<TabEntry> = mutableListOf(
            EmojiDataController,
        )
    }
}