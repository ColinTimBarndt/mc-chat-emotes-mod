@file:JvmName("AssetsKt")
@file:Suppress("NOTHING_TO_INLINE")

package io.github.colintimbarndt.chat_emotes_util

import io.github.colintimbarndt.chat_emotes_util.fx.App
import java.io.InputStream
import java.net.URL

inline fun getAsset(name: String): URL? = App::class.java.getResource(name)

inline fun streamAsset(name: String): InputStream? = App::class.java.getResourceAsStream(name)
