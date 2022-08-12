package io.github.colintimbarndt.chat_emotes.command

import io.github.colintimbarndt.chat_emotes.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.common.commands.ChatEmotesCommandBase
import java.util.*

object ChatEmotesCommand : ChatEmotesCommandBase() {
    override val serverMod get() = ChatEmotesServerMod

    override fun getIcon(): Optional<String> = ChatEmotesServerMod.modMetadata.getIconPath(128)
}
