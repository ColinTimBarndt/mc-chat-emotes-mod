package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.serial.PackFormat
import javafx.scene.control.ChoiceBox

class GameVersionChoiceBox : ChoiceBox<PackFormat>() {
    init {
        items.setAll(PackFormat.values)
        value = PackFormat.values.last()
    }
}