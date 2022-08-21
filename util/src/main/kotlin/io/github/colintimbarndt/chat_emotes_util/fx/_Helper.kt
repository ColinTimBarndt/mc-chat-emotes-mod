@file:JvmName("HelperKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.DefaultedEnum
import io.github.colintimbarndt.chat_emotes_util.Labeled
import io.github.colintimbarndt.chat_emotes_util.emojidata.FontNaming
import io.github.colintimbarndt.chat_emotes_util.emojidata.FontNamingFactory
import io.github.colintimbarndt.chat_emotes_util.emojidata.TextureUsageRights
import io.github.colintimbarndt.chat_emotes_util.getAsset
import io.github.colintimbarndt.chat_emotes_util.serial.FileType
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.control.ChoiceBox
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import javafx.scene.text.Text
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javafx.util.StringConverter
import javafx.util.converter.IntegerStringConverter
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.function.UnaryOperator

val jsonExtensionFilter = ExtensionFilter("JSON Files", "*.json")
val zipExtensionFilter = ExtensionFilter("ZIP Files", "*.zip")

inline fun <T> loadFXML(src: String): T = FXMLLoader.load(App::class.java.getResource(src))

inline fun <T> loadFXML(src: String, rootCtrl: Any): T {
    val loader = FXMLLoader(getAsset(src))
    loader.setRoot(rootCtrl)
    loader.setController(rootCtrl)
    return loader.load()
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

fun <T : Labeled> ChoiceBox<T>.setItemsEnum(enum: DefaultedEnum<T>) {
    converter = labeledConverter()
    items.setAll(*enum.values())
    value = enum.default
}

private object PositiveIntFilter : UnaryOperator<TextFormatter.Change?> {
    private val intPattern = Regex("\\d+")
    private inline fun String.isInt() = matches(intPattern)
    override fun apply(change: TextFormatter.Change?): TextFormatter.Change? {
        if (change == null) return null
        if (change.text.isBlank()) return change
        if (change.controlNewText.isInt()) return change
        return null
    }
}

private val intStringConverter = IntegerStringConverter()

private class NumberInputControlHandler(
    private val formatter: TextFormatter<Int>
) : EventHandler<KeyEvent> {
    var lastInput = 0L
    fun resetLastInput() {
        lastInput = 0L
    }

    override fun handle(event: KeyEvent) {
        val thisInput = System.currentTimeMillis()
        if (thisInput - lastInput < 200) return
        lastInput = thisInput
        val invert = when (event.code) {
            KeyCode.UP -> false
            KeyCode.DOWN -> true
            else -> return
        }
        event.consume()
        formatter.value = arrowModifiedValue(event, formatter.value, invert)
    }

    private inline fun arrowModifiedValue(event: KeyEvent, value: Int, invert: Boolean, maxValue: Int = 2 shl 14): Int {
        if (event.isControlDown) {
            // Next power of 2
            val v = value.takeHighestOneBit()
            return if (invert) {
                if (value == v) v shr 1
                else v
            } else if (value == 0) 1
            else min(maxValue, (v shl 1))
        }
        var mag = if (invert) -1 else 1
        if (event.isShiftDown) mag *= 10
        return max(0, min(maxValue, value + mag))
    }
}

fun TextField.formatAsPositiveInt(default: Int = 0): TextFormatter<Int> {
    val formatter = TextFormatter(intStringConverter, default, PositiveIntFilter)
    textFormatter = formatter
    styleClass.add("numeric")
    val handler = NumberInputControlHandler(formatter)
    onKeyPressed = handler
    setOnKeyReleased { handler.resetLastInput() }
    return formatter
}

inline fun DirectoryChooser.showDialog(): File? = showDialog(App.INSTANCE.stage)
inline fun FileChooser.showSaveDialog(): File? = showSaveDialog(App.INSTANCE.stage)

private fun fileDialog(
    title: String,
    filter: ExtensionFilter,
    fileName: String?,
    initialDirectory: File?
): FileChooser {
    return FileChooser().also {
        it.title = title
        it.initialDirectory = initialDirectory
        it.extensionFilters.setAll(filter)
        it.initialFileName = fileName
    }
}

fun FileType.showSaveDialog(title: String, fileName: String? = null, initialDirectory: File? = null): File? {
    return when (this) {
        FileType.Folder -> {
            DirectoryChooser().also {
                it.title = title
                it.initialDirectory = initialDirectory
            }.showDialog()
        }

        FileType.Json -> fileDialog(title, jsonExtensionFilter, fileName, initialDirectory).showSaveDialog()
        FileType.Zip -> fileDialog(title, zipExtensionFilter, fileName, initialDirectory).showSaveDialog()
    }
}

fun displayUsageRights(rights: TextureUsageRights, text: Text, buttons: Pane) {
    text.text = rights.message
    buttons.children.clear()
    for ((i, source) in rights.sources().withIndex()) {
        buttons.children.add(
            LinkButton(
                "Source ${i + 1}",
                uri = source.userUri.toASCIIString()
            )
        )
    }
}