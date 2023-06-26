@file:JvmName("ConvertKt")

package io.github.colintimbarndt.chat_emotes.common.mod.util

import net.minecraft.resources.ResourceLocation
import io.github.colintimbarndt.chat_emotes.common.data.ResourceLocation as CEResourceLocation

fun CEResourceLocation.toMinecraft() =
    ResourceLocation(this.namespace, this.path)