package io.github.colintimbarndt.chat_emotes.mixin;

import io.github.colintimbarndt.chat_emotes.ChatEmotesMod;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class ExampleMixin {
	@Inject(at = @At("HEAD"), method = "init()V")
	private void init(CallbackInfo info) {
		ChatEmotesMod.LOGGER.info("This line is printed by an example mod mixin!");
	}
}
