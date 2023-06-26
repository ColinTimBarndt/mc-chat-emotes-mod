@file:Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.MOD)

package io.github.colintimbarndt.chat_emotes.forge

import io.github.colintimbarndt.chat_emotes.common.LOGGER
import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.forge.config.ForgeChatEmotesConfig
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.config.ModConfigEvent

@SubscribeEvent
internal fun onReloadConfig(event: ModConfigEvent.Reloading) {
    LOGGER.info("RELOAD CONFIG ${event.config}")
    ChatEmotesServerMod.config = ForgeChatEmotesConfig.INSTANCE.unmodifiable
}