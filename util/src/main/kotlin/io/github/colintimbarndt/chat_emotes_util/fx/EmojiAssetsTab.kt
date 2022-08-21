package io.github.colintimbarndt.chat_emotes_util.fx

import io.github.colintimbarndt.chat_emotes_util.LOGGER
import io.github.colintimbarndt.chat_emotes_util.emojidata.*
import io.github.colintimbarndt.chat_emotes_util.serial.*
import io.github.colintimbarndt.chat_emotes_util.web.WebHelper.STANDARD_CACHE_TIME
import io.github.colintimbarndt.chat_emotes_util.web.getInputStream
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.text.Text
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.CountDownLatch
import kotlin.math.max

class EmojiAssetsTab : BorderPane() {
    init {
        loadFXML<Node>("/scenes/EmojiAssetsTab.fxml", this)
    }

    @FXML
    lateinit var exportButton: MenuButton

    @FXML
    lateinit var gameVersionChoice: ChoiceBox<PackFormat>

    @FXML
    lateinit var textureSource: ChoiceBox<TextureLoader>

    @FXML
    lateinit var variantChoice: ChoiceBox<String>

    @FXML
    lateinit var resolutionChoice: ChoiceBox<Int>

    @FXML
    lateinit var emojiSourcesForm: EmojiSourcesForm

    @FXML
    lateinit var targetResolutionText: TextField
    lateinit var targetResolution: TextFormatter<Int>

    @FXML
    lateinit var atlasSizeText: TextField
    lateinit var atlasSize: TextFormatter<Int>

    @FXML
    lateinit var atlasNamespace: TextField

    @FXML
    lateinit var atlasName: TextField

    @FXML
    lateinit var fontNameType: ChoiceBox<FontNamingFactory>

    @FXML
    lateinit var fontNamespace: TextField

    @FXML
    lateinit var fontName: TextField

    @FXML
    lateinit var prettyCheck: CheckBox

    @FXML
    lateinit var packMetaCheck: CheckBox

    @FXML
    lateinit var textureRightsText: Text

    @FXML
    lateinit var rightSourceButtons: Pane

    @FXML
    @Suppress("UNUSED")
    fun initialize() {
        gameVersionChoice.items.setAll(PackFormat.values)
        gameVersionChoice.value = PackFormat.values.last()

        emojiSourcesForm.nameChoice.value = EmojiNameSource.Raw
        emojiSourcesForm.aliasesChoice.value = EmojiAliasSource.None

        targetResolution = targetResolutionText.formatAsPositiveInt()
        atlasSize = atlasSizeText.formatAsPositiveInt(8)

        resolutionChoice.setOnAction {
            targetResolution.value = resolutionChoice.value
        }

        variantChoice.setOnAction {
            val variant = variantChoice.value
            val textureSrc = textureSource.value
            val rights = textureSrc.variants.usageRightsFor(variant)
            displayUsageRights(rights, textureRightsText, rightSourceButtons)
            atlasName.text = (textureSrc.variants.shortNameFor(variant) ?: variant).lowercase()
        }

        textureSource.converter = labeledConverter()
        textureSource.items.setAll(TextureLoader.EMOJI_TEXTURES)
        textureSource.setOnAction {
            val source = textureSource.value!!
            run {
                // Update resolution settings
                val prevRes = resolutionChoice.value
                resolutionChoice.items.setAll(*source.sizes.toTypedArray())
                // If possible, don't change the resolution setting
                resolutionChoice.value = if (
                    prevRes == null
                    || source.sizes.binarySearch(prevRes) < 0
                ) source.defaultSize
                else prevRes
            }
            run {
                // Update variant settings
                val prevVar = variantChoice.value
                variantChoice.items.setAll(source.variants.values)
                // If possible, don't change the variant setting
                variantChoice.value = if (
                    prevVar == null
                    || prevVar !in source.variants.values
                ) source.defaultVariant
                else prevVar
            }
        }
        textureSource.value = TextureLoader.EMOJI_TEXTURES.first()

        fontNameController(fontNameType, fontName)

        for (writerFactory in PackWriter.values) {
            val menuItem = MenuItem(writerFactory.label)
            menuItem.setOnAction {
                exportResourcePack(writerFactory)
            }
            exportButton.items.add(menuItem)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun exportResourcePack(writerFactory: PackWriterFactory) {
        exportButton.isDisable = true
        val variant = variantChoice.value!!
        val textureSrc = textureSource.value!!
        val shortName = textureSrc.variants.shortNameFor(variant) ?: variant
        val file = writerFactory.fileType.showSaveDialog(
            title = "Save Resource Pack",
            fileName = shortName + writerFactory.fileType.extension
        )
        if (file == null) {
            exportButton.isDisable = false
            return
        }
        val job = GlobalScope.launch {
            LOGGER.info("Writing resource pack to {}", file.absolutePath)
            val sourceSize = resolutionChoice.value!!
            val data = EmojiDataProvider.loadSequence()
                .flatEmojiData()
                .expandToEmoteData(
                    emojiSourcesForm.loadSources(),
                    fontNamespace.text,
                    fontNameType.value.create(fontName.text)
                )
            val textures = withContext(Dispatchers.IO) {
                textureSrc.load(sourceSize, variant)
            }
            val json = if (prettyCheck.isSelected) Json { prettyPrint = true } else Json
            val glyphOptions = FontAssetOptions(
                glyphSize = targetResolution.value,
                atlasSize = max(1, atlasSize.value),
                json = json
            )
            writerFactory.of(file).use { packWriter ->
                if (packMetaCheck.isSelected) {
                    packWriter.addMetadata(json) {
                        pack.format = gameVersionChoice.value.resourceFormat
                        pack.description = buildJsonObject {
                            put("text", "$shortName Chat Emotes")
                            put("color", "yellow")
                        }
                    }
                }
                val lock = CountDownLatch(1)
                Platform.runLater {
                    // writeFonts uses JavaFX for image processing
                    try {
                        writeFonts(
                            packWriter,
                            data,
                            textures,
                            glyphOptions,
                            atlasNamespace.text,
                            atlasName.text,
                        )
                    } finally {
                        lock.countDown()
                    }
                }
                withContext(Dispatchers.IO) { lock.await() }
                val rights = textureSrc.variants.usageRightsFor(variant)
                for ((i, source) in rights.sources().withIndex()) {
                    val uri = source.uri
                    val path = uri.path
                    val ext = path.substringAfterLast('.', "txt")
                    val idx = if (i > 0) i.toString() else ""
                    packWriter.addFile(
                        "LICENSE$idx-$shortName.$ext",
                        source.getInputStream(STANDARD_CACHE_TIME).result
                    )
                }
            }
        }
        job.invokeOnCompletion {
            LOGGER.info("Done writing resource pack")
            exportButton.isDisable = false
        }
    }
}