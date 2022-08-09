@file:JvmName("LanguageMixin")
@file:Mixin(Language::class)

package io.github.colintimbarndt.chat_emotes.mixin

import com.google.common.collect.ImmutableMap
import com.google.gson.JsonParseException
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod.Companion.MOD_ID
import net.minecraft.locale.Language
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.callback.LocalCapture
import java.io.FileNotFoundException
import java.io.IOException
import java.util.function.BiConsumer

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
) {
    val path = "/assets/$MOD_ID/lang/en_us.json"
    try {
        ChatEmotesMod::class.java.getResourceAsStream(path).use { stream ->
            if (stream == null) throw FileNotFoundException()
            Language.loadFromJson(stream) { k: String, v: String -> if (!k.startsWith('%')) keyAdder.accept(k, v) }
        }
    } catch (ex: JsonParseException) {
        ChatEmotesMod.LOGGER.error("Couldn't parse strings from {}", path, ex)
    } catch (ex: IOException) {
        ChatEmotesMod.LOGGER.error("Couldn't read strings from {}", path, ex)
    }
}