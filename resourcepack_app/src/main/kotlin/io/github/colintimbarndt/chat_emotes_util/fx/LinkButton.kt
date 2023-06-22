package io.github.colintimbarndt.chat_emotes_util.fx

import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Tooltip

class LinkButton(
    text: String? = null,
    graphic: Node? = null,
    uri: String? = null,
) : Button(text, graphic) {
    private var _uri: String?
    init {
        _uri = uri
        tooltip = Tooltip(uri)
    }

    var uri: String?
        get() = _uri
        set(value) {
            _uri = value
            tooltip.text = value
        }

    init {
        setOnAction {
            if (uri != null) {
                App.INSTANCE.hostServices.showDocument(uri)
            }
        }
    }
}