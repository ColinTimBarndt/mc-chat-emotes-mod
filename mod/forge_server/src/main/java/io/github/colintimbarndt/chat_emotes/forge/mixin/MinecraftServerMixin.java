package io.github.colintimbarndt.chat_emotes.forge.mixin;

import io.github.colintimbarndt.chat_emotes.forge.ServerMessageDecoratorEvent;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "getChatDecorator", at = @At("RETURN"), cancellable = true)
    private void onGetChatDecorator(CallbackInfoReturnable<ChatDecorator> cir) {
        final ChatDecorator originalDecorator = cir.getReturnValue();
        cir.setReturnValue((sender, message) ->
                originalDecorator.decorate(sender, message)
                        .thenApply((decorated) -> ServerMessageDecoratorEvent.Companion.decorate(
                                sender,
                                decorated)
                        )
        );
    }
}
