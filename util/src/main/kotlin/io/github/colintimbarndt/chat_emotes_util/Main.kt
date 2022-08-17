@file:JvmName("Main")

package io.github.colintimbarndt.chat_emotes_util

import javafx.application.Application
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val LOGGER: Logger = LoggerFactory.getLogger("Chat Emotes Util")

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}
