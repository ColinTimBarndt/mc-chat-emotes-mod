package io.github.colintimbarndt.chat_emotes.common.commands

import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractCommandAdapter
import io.github.colintimbarndt.chat_emotes.common.abstraction.AbstractComponentFactory
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.fallbackTranslatable

abstract class ChatEmotesCommandBase<S, Ctx, Component>(
    private val componentFactory: AbstractComponentFactory<Component>,
    private val commandAdapter: AbstractCommandAdapter<S, Ctx, Component>,
) {
    protected abstract fun performReloadConfig(): Boolean

    protected fun tryReloadConfig(context: Ctx): Int = commandAdapter.run {
        return if (performReloadConfig()) {
            context.sendSuccess(
                componentFactory.fallbackTranslatable("commands.chat_emotes.reload.success").build(),
                true
            )
            1
        } else {
            context.sendFailure(
                componentFactory.fallbackTranslatable("commands.chat_emotes.reload.failure").build()
            )
            0
        }
    }
}