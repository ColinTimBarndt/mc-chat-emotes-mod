package io.github.colintimbarndt.chat_emotes_util.fx

import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.Tab
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LazyTab : Tab() {
    init {
        loadFXML<Tab>("/scenes/LazyTab.fxml", this)
    }

    // Tagged Union
    private var contentType = ContentType.Empty
    private var _content: Any? = null

    @get:Suppress("UNCHECKED_CAST")
    var component: Class<out Node>?
        get() = if (contentType == ContentType.ComponentClass) _content as Class<out Node> else null
        set(value) {
            value ?: run {
                contentType = ContentType.Empty
                _content = null
                return
            }
            contentType = ContentType.ComponentClass
            _content = value
            if (isSelected) return loadComponent(value)
            setOnSelectionChanged {
                if (isSelected) {
                    onSelectionChanged = null
                    loadComponent(value)
                }
            }
        }

    var fxml: String?
        get() = if (contentType == ContentType.FXML) _content as String else null
        set(value) {
            value ?: run {
                contentType = ContentType.Empty
                _content = null
                return
            }
            contentType = ContentType.FXML
            _content = value
            if (isSelected) return loadFxml(value)
            setOnSelectionChanged {
                if (isSelected) {
                    onSelectionChanged = null
                    loadFxml(value)
                }
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadComponent(component: Class<out Node>) {
        GlobalScope.launch(Dispatchers.IO) {
            val comp = component.getConstructor().newInstance()
            Platform.runLater {
                content = comp
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadFxml(src: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val comp = loadFXML<Node>(src)
            Platform.runLater {
                content = comp
            }
        }
    }

    private enum class ContentType {
        Empty,
        ComponentClass,
        FXML
    }
}