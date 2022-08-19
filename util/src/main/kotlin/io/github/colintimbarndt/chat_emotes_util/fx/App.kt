package io.github.colintimbarndt.chat_emotes_util.fx

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class App : Application() {
    companion object {
        lateinit var INSTANCE: App
    }

    init {
        INSTANCE = this
    }

    lateinit var stage: Stage private set

    override fun start(stage: Stage) {
        this.stage = stage
        val root: Parent = FXMLLoader.load(App::class.java.getResource("/scenes/Root.fxml"))
        stage.scene = Scene(root)
        stage.title = "Chat Emotes Utility"
        stage.minWidth = 450.0
        stage.minHeight = 200.0
        stage.show()
    }
}