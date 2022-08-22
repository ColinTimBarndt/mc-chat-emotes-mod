package io.github.colintimbarndt.chat_emotes_util.fx

import javafx.scene.Node

interface TabEntry {
    val title: String
    fun createBodyNode(): Node
}