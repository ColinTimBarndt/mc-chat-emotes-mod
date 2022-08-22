@file:JvmName("FuturesKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes.common.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object Futures {
    inline fun <T> supplyAsync(executor: Executor, noinline block: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(block, executor)
}