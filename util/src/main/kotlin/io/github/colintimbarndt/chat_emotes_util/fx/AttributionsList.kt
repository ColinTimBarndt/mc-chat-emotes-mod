@file:OptIn(ExperimentalSerializationApi::class, DelicateCoroutinesApi::class)

package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.model.Attribution
import io.github.colintimbarndt.chat_emotes_util.streamAsset
import javafx.application.Platform
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.w3c.dom.Node

class AttributionsList : VBox() {
    lateinit var grid: GridPane

    init {
        loadFXML<Node>("/scenes/AttributionsList.fxml", this)

        GlobalScope.launch(Dispatchers.IO) {
            val attributions = Json.decodeFromStream<List<Attribution>>(
                streamAsset("/assets/attributions.json")!!
            )
            Platform.runLater {
                attributions.withIndex().forEach { (row, attribution) ->
                    grid.addRow(
                        row,
                        Text(attribution.name),
                        LinkButton("Source", uri = attribution.source.userUri.toASCIIString())
                    )
                    if (attribution.license != null) {
                        grid.add(
                            LinkButton("License", uri = attribution.license.userUri.toASCIIString()), 2, row
                        )
                    }
                }
            }
        }
    }
}