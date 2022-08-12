@file:JvmName("LanguageMixin")
@file:Mixin(Language::class)

package io.github.colintimbarndt.chat_emotes.mixin

import com.google.common.collect.ImmutableMap
import io.github.colintimbarndt.chat_emotes.ChatEmotesServerMod
import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.common.mixin.attemptLoadModLanguage
import net.minecraft.locale.Language
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.callback.LocalCapture
import java.util.function.BiConsumer

private const val langPath = "/assets/$MOD_ID/lang/en_us.json"

@Inject(
    method = ["loadDefault"],
    at = [At(
        value = "INVOKE",
        target = "Lnet/minecraft/locale/Language;loadFromJson",
        shift = At.Shift.AFTER
    )],
    locals = LocalCapture.CAPTURE_FAILSOFT
)
private fun loadModTranslations(
    cir: CallbackInfoReturnable<Language>,
    builder: ImmutableMap.Builder<String, String>,
    keyAdder: BiConsumer<String, String>
) = attemptLoadModLanguage(ChatEmotesServerMod::class.java, langPath, keyAdder)
