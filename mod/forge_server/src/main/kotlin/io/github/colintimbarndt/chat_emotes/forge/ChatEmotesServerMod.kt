package io.github.colintimbarndt.chat_emotes.forge

import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.common.mod.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.mod.permissions.VanillaPermissionsAdapter
import io.github.colintimbarndt.chat_emotes.forge.config.ForgeChatEmotesConfig
import io.github.colintimbarndt.chat_emotes.forge.data.EmoteDataLoader
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig

@Mod(MOD_ID)
object ChatEmotesServerMod : ChatEmotesServerModBase() {
    override val emoteDataLoader = EmoteDataLoader

    init {
        val ctx = ModLoadingContext.get()
        ctx.registerConfig(ModConfig.Type.COMMON, ForgeChatEmotesConfig.SPEC)

        permissionsAdapter = VanillaPermissionsAdapter
    }
}