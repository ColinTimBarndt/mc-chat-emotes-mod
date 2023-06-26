package io.github.colintimbarndt.chat_emotes.fabric.config

import io.github.colintimbarndt.chat_emotes.fabric.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import net.minecraft.locale.Language
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds

class ConfigWatcher() : Thread("Chat Emotes Conf-Watcher") {

    @Throws(IOException::class)
    override fun run() {
        val configPath = ChatEmotesServerMod.configPath
        FileSystems.getDefault().newWatchService().use { watcher ->
            configPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)
            try {
                while (true) {
                    val wk = watcher.take()
                    val changed = wk.pollEvents().isNotEmpty()
                    if (changed) reloadConfig()
                    wk.reset()
                }
            } catch (_: InterruptedException) {
                return
            }
        }
    }

    private fun reloadConfig() {
        val success = ChatEmotesServerMod.reloadConfig()
        val msgKey =
            if (success) "commands.chat_emotes.reload.success"
            else "commands.chat_emotes.reload.failure"
        LOGGER.info(Language.getInstance().getOrDefault(msgKey))
    }
}