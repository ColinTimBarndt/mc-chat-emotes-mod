package io.github.colintimbarndt.chat_emotes.common.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.colintimbarndt.chat_emotes.common.ChatEmotesServerModBase
import io.github.colintimbarndt.chat_emotes.common.LOGGER
import io.github.colintimbarndt.chat_emotes.common.MOD_ID
import io.github.colintimbarndt.chat_emotes.common.data.PackExportException
import io.github.colintimbarndt.chat_emotes.common.util.ComponentUtils.fallbackTranslatable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class ChatEmotesCommandBase {
    protected abstract val serverMod: ChatEmotesServerModBase

    /**
     * Gets the icon used when generating a resource pack.
     * This is a path to the mod icon file in the mod classloader
     */
    protected abstract fun getIcon(): Optional<String>

    fun tryReloadConfig(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        return if (serverMod.reloadConfig()) {
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
        val config = serverMod.config
        for (pack in config.resourcepacks) {
            if (pack.name == packName) {
                val emotes = serverMod.emoteDataLoader.loadedEmoteData
                try {
                    pack.export(serverMod, emotes, getIcon())
                } catch (ex: PackExportException) {
                    context.source.sendFailure(
                        fallbackTranslatable("commands.chat_emotes.export.failure", ex.message)
                    )
                    return 0
                } catch (ex: IOException) {
                    LOGGER.error("Failed to export emotes", ex)
                    context.source.sendFailure(
                        fallbackTranslatable("commands.chat_emotes.export.failure.other")
                    )
                    return 0
                } catch (t: Throwable) {
                    LOGGER.error("Failed to export emotes, unexpected error", t)
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
        val config = serverMod.config
        for (pack in config.resourcepacks) {
            builder.suggest(StringArgumentType.escapeIfRequired(pack.name))
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
            Commands.literal(MOD_ID)
                .then(reload)
                .then(export)
        )
    }
}