package io.github.colintimbarndt.chat_emotes.mixin;

import io.github.colintimbarndt.chat_emotes.EmoteDecorator;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.server.MinecraftServer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public class MessageDecoratorMixin {
    @Redirect(
            method = "getMessageDecorator()Lnet/minecraft/network/message/MessageDecorator;",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/network/message/MessageDecorator;NOOP:Lnet/minecraft/network/message/MessageDecorator;",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private MessageDecorator modifyMessageDecorator() {
        return EmoteDecorator.EMOTES;
    }
}
