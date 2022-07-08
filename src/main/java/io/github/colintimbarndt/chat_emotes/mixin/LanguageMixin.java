package io.github.colintimbarndt.chat_emotes.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.BiConsumer;

import static io.github.colintimbarndt.chat_emotes.ChatEmotesMod.LOGGER;
import static io.github.colintimbarndt.chat_emotes.ChatEmotesMod.MODID;

@Mixin(Language.class)
public class LanguageMixin {
    @Inject(
            method = "create()Lnet/minecraft/util/Language;",
            at = @At(
                    value = "INVOKE",
                    target = "load(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void loadModTranslations(
            CallbackInfoReturnable<Language> cir,
            ImmutableMap.Builder<String, String> builder,
            BiConsumer<String, String> keyAdder
    ) {
        final var path = "/assets/" + MODID + "/lang/en_us.json";
        try (final var stream = ChatEmotesMod.class.getResourceAsStream(path)) {
            if (stream == null) throw new FileNotFoundException();
            Language.load(stream, (k, v) -> {
                if (!k.startsWith("%")) keyAdder.accept(k, v);
            });
        } catch (JsonParseException | IOException ex) {
            LOGGER.error("Couldn't read strings from {}", path, ex);
        }
    }
}
