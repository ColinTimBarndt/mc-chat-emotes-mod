package io.github.colintimbarndt.chat_emotes.mixin;

import java.util.function.BiConsumer;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Language.class)
final class LanguageMixin {

    @ModifyVariable(
            method = "loadFromJson(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static BiConsumer<String, String> filterTranslations(
            final BiConsumer<String, String> keyAdder
    ) {
        return (k, v) -> {
            if (k.equals("chat.emote")) {
                return;
            }
            keyAdder.accept(k, v);
        };
    }
}
