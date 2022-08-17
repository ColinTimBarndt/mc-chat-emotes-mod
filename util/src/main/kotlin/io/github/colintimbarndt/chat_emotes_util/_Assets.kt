@file:JvmName("AssetsKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util

import java.io.InputStream
import java.net.URI

internal inline fun streamAsset(name: String): InputStream? = App::class.java.getResourceAsStream(name)

internal fun lazyStringAsset(name: String) = lazy {
    streamAsset(name)!!.reader().use { it.readText() }
}
