package io.github.colintimbarndt.chat_emotes.common

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation

@Suppress("NOTHING_TO_INLINE")
open class Registries {
    inline fun <T> Registry<T>.register(key: String, value: T) {
        Registry.register(this, ResourceLocation(NAMESPACE, key), value)
    }
    inline fun <T> Registry<T>.register(location: ResourceLocation, value: T) {
        Registry.register(this, location, value)
    }
}
