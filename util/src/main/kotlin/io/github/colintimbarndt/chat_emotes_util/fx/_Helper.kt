@file:JvmName("HelperKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.emojidata.FontNaming
import io.github.colintimbarndt.chat_emotes_util.emojidata.FontNamingFactory
import io.github.colintimbarndt.chat_emotes_util.serial.FileType
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Tab
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.util.StringConverter
import java.io.File

val jsonExtensionFilter = FileChooser.ExtensionFilter("JSON Files", "*.json")
val zipExtensionFilter = FileChooser.ExtensionFilter("ZIP Files", "*.zip")

inline fun setAnchorInset(node: Node, v: Double) {
    setAnchorHInset(node, v)
    AnchorPane.setTopAnchor(node, v)
    AnchorPane.setBottomAnchor(node, v)
}

inline fun setAnchorHInset(node: Node, v: Double) {
    AnchorPane.setLeftAnchor(node, v)
    AnchorPane.setRightAnchor(node, v)
}

inline fun <T : Node> loadFXML(src: String): T = FXMLLoader.load(App::class.java.getResource(src))

internal inline fun lazyLoadTab(tab: Tab, crossinline load: Tab.() -> Unit) {
    if (tab.isSelected) return tab.load()
    tab.setOnSelectionChanged {
        if (tab.isSelected) {
            tab.onSelectionChanged = null
            tab.load()
        }
    }
}

internal fun fontNameController(
    fontNameType: ChoiceBox<FontNamingFactory>,
    templateField: TextField,
) {
    fontNameType.setOnAction {
        templateField.text = fontNameType.value.example
    }

    fontNameType.converter = labeledConverter()
    fontNameType.items.setAll(FontNaming.values)
    fontNameType.selectionModel.select(FontNaming.default)
}

private object LabeledConverter : StringConverter<Labeled>() {
    override fun toString(obj: Labeled?) = obj?.label
    override fun fromString(string: String?) = null
}

@Suppress("UNCHECKED_CAST")
fun <T : Labeled> labeledConverter() = LabeledConverter as StringConverter<T>

inline fun DirectoryChooser.showDialog(): File? = showDialog(App.INSTANCE.stage)
inline fun FileChooser.showSaveDialog(): File? = showSaveDialog(App.INSTANCE.stage)

private fun fileDialog(title: String, filter: ExtensionFilter, initialDirectory: File?): FileChooser {
    return FileChooser().also {
        it.title = title
        it.initialDirectory = initialDirectory
        it.extensionFilters.setAll(filter)
    }
}

fun FileType.showSaveDialog(title: String, initialDirectory: File? = null): File? {
    return when (this) {
        FileType.Folder -> {
            DirectoryChooser().also {
                it.title = title
                it.initialDirectory = initialDirectory
            }.showDialog()
        }
        FileType.Json -> fileDialog(title, jsonExtensionFilter, initialDirectory).showSaveDialog()
        FileType.Zip -> fileDialog(title, zipExtensionFilter, initialDirectory).showSaveDialog()
    }
}