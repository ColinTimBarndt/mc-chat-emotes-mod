package io.github.colintimbarndt.chat_emotes.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod.Companion.config
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod.Companion.reloadConfig
import io.github.colintimbarndt.chat_emotes.data.PackExportException
import io.github.colintimbarndt.chat_emotes.util.ComponentUtils.fallbackTranslatable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import java.io.IOException
import java.util.concurrent.CompletableFuture

object ChatEmotesCommand {
    fun tryReloadConfig(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        return if (reloadConfig()) {
            source.sendSuccess(
                fallbackTranslatable("commands.chat_emotes.reload.success"),
                true
            )
            1
        } else {
            source.sendFailure(
                fallbackTranslatable("commands.chat_emotes.reload.failure")
            )
            0
        }
    }

    fun tryExportResourcepacks(context: CommandContext<CommandSourceStack>): Int {
        val packName = StringArgumentType.getString(context, "pack name")
        val config = config
        if (config == null) {
            context.source.sendFailure(fallbackTranslatable("commands.chat_emotes.export.failure.no_config"))
            return 0
        }
        for (pack in config.resourcepacks) {
            if (pack.name == packName) {
                val emotes = ChatEmotesMod.EMOTE_DATA_LOADER.loadedEmoteData
                try {
                    pack.export(emotes)
                } catch (ex: PackExportException) {
                    context.source.sendFailure(
                        fallbackTranslatable("commands.chat_emotes.export.failure", ex.message)
                    )
                    return 0
                } catch (ex: IOException) {
                    ChatEmotesMod.LOGGER.error("Failed to export emotes", ex)
                    context.source.sendFailure(
                        fallbackTranslatable("commands.chat_emotes.export.failure.other")
                    )
                    return 0
                } catch (t: Throwable) {
                    ChatEmotesMod.LOGGER.error("Failed to export emotes, unexpected error", t)
                    context.source.sendFailure(
                        fallbackTranslatable("commands.chat_emotes.export.failure.other")
                    )
                    return 0
                }
                context.source.sendSuccess(
                    fallbackTranslatable("commands.chat_emotes.export.success"),
                    true
                )
                return 1
            }
        }
        context.source.sendFailure(fallbackTranslatable("commands.chat_emotes.export.failure.no_pack"))
        return 0
    }

    private fun suggestResourcepackNames(
        context: CommandContext<CommandSourceStack>?,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val config = config
        if (config != null) {
            for (pack in config.resourcepacks) {
                builder.suggest(StringArgumentType.escapeIfRequired(pack.name))
            }
        }
        return CompletableFuture.completedFuture(builder.build())
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val reload = Commands.literal("reload")
            .requires { source: CommandSourceStack -> source.hasPermission(2) }
            .executes(::tryReloadConfig)
        val export = Commands.literal("export")
            .requires { source: CommandSourceStack -> source.hasPermission(4) }
            .then(
                Commands.argument("pack name", StringArgumentType.string())
                    .suggests(::suggestResourcepackNames)
                    .executes(::tryExportResourcepacks)
            )
        dispatcher.register(
            Commands.literal(ChatEmotesMod.MOD_ID)
                .then(reload)
                .then(export)
        )
    }
}