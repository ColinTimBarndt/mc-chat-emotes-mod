package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.streamAsset
import javafx.application.Platform
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LazyTextFlow : TextFlow() {
    private var _source: String? = null
    @set:OptIn(DelicateCoroutinesApi::class)
    var source
        get() = _source
        set(value) {
            _source = value
            value ?: return
            GlobalScope.launch(Dispatchers.IO) {
                val text = streamAsset(value)!!.reader().readText()
                Platform.runLater {
                    children.setAll(Text(text))
                }
            }
        }
}