@file:Mod.EventBusSubscriber(modid = MOD_ID, bus = Bus.FORGE)

package io.github.colintimbarndt.chat_emotes.forge

import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.forge.data.EmoteDataLoader
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

@SubscribeEvent
internal fun decorate(event: ServerMessageDecoratorEvent) {
    event.message = ChatEmotesServerMod.emoteDecorator.decorateSync(event.sender, event.message)
}

@SubscribeEvent
internal fun chat(event: ServerChatEvent) {
    event.message = ServerMessageDecoratorEvent.decorate(event.player, event.message)
}

@SubscribeEvent
internal fun reload(event: AddReloadListenerEvent) {
    event.addListener(EmoteDataLoader)
}