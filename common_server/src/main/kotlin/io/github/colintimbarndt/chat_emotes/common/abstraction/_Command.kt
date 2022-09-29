@file:JvmName("CommandKt")

package io.github.colintimbarndt.chat_emotes.common.abstraction

abstract class AbstractCommandAdapter<S, Ctx, Component> {
    abstract fun Ctx.sendSuccess(message: Component, broadcast: Boolean)
    abstract fun Ctx.sendFailure(message: Component)
    abstract val Ctx.commandSource: S
}