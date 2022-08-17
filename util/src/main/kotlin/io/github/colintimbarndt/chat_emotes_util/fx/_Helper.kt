@file:JvmName("HelperKt")

package io.github.colintimbarndt.chat_emotes_util.fx

import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.layout.AnchorPane

fun setAnchorInset(node: Node, v: Double) {
    setAnchorHInset(node, v)
    AnchorPane.setTopAnchor(node, v)
    AnchorPane.setBottomAnchor(node, v)
}

fun setAnchorHInset(node: Node, v: Double) {
    AnchorPane.setLeftAnchor(node, v)
    AnchorPane.setRightAnchor(node, v)
}

fun lazyLoadTab(tab: Tab, load: () -> Unit) {
    tab.onSelectionChanged = object : EventHandler<Event> {
        var mustLoad = true

        override fun handle(event: Event) {
            if (tab.isSelected && mustLoad) {
                mustLoad = false
                load()
            }
        }
    }
}