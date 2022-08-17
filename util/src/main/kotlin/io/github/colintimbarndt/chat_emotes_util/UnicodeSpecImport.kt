package io.github.colintimbarndt.chat_emotes_util

import io.github.colintimbarndt.chat_emotes_util.fx.TabEntry
import javafx.fxml.FXMLLoader
import javafx.scene.Node

object UnicodeSpecImport : TabEntry {
    override val title = "Unicode Spec"

    override fun createBodyNode(): Node =
        FXMLLoader.load(App::class.java.getResource("/scenes/UnicodeSpecImport.fxml"))

}