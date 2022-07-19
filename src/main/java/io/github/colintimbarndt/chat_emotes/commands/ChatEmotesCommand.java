package io.github.colintimbarndt.chat_emotes.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod;
import io.github.colintimbarndt.chat_emotes.config.ResourcepackConfig;
import io.github.colintimbarndt.chat_emotes.data.PackExportException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static io.github.colintimbarndt.chat_emotes.TranslationHelper.fallbackTranslatable;
import static net.minecraft.commands.Commands.*;
import static io.github.colintimbarndt.chat_emotes.ChatEmotesMod.*;

public final class ChatEmotesCommand {
    private ChatEmotesCommand() {}
    public static int tryReloadConfig(CommandContext<CommandSourceStack> context) {
        final var source = context.getSource();
        if (ChatEmotesMod.reloadConfig()) {
            source.sendSuccess(
                    fallbackTranslatable("commands.chat_emotes.reload.success"),
                    true
            );
            return 1;
        } else {
            source.sendFailure(
                    fallbackTranslatable("commands.chat_emotes.reload.failure")
            );
            return 0;
        }
    }

    public static int tryExportResourcepacks(CommandContext<CommandSourceStack> context) {
        final String packName = StringArgumentType.getString(context, "pack name");
        final var config = ChatEmotesMod.getConfig();
        if (config == null) {
            context.getSource().sendFailure(fallbackTranslatable("commands.chat_emotes.export.failure.no_config"));
            return 0;
        }
        for (ResourcepackConfig pack : config.getResourcepacks()) {
            if (pack.name().equals(packName)) {
                final var emotes = ChatEmotesMod.EMOTE_DATA_LOADER.getLoadedEmoteData();
                try {
                    pack.export(emotes);
                } catch (PackExportException ex) {
                    context.getSource().sendFailure(
                            fallbackTranslatable("commands.chat_emotes.export.failure", ex.getMessage())
                    );
                    return 0;
                } catch (IOException ex) {
                    LOGGER.error("Failed to export emotes", ex);
                    context.getSource().sendFailure(
                            fallbackTranslatable("commands.chat_emotes.export.failure.other")
                    );
                    return 0;
                } catch (Throwable t) {
                    LOGGER.error("Failed to export emotes, unexpected error", t);
                    context.getSource().sendFailure(
                            fallbackTranslatable("commands.chat_emotes.export.failure.other")
                    );
                    return 0;
                }
                context.getSource().sendSuccess(
                        fallbackTranslatable("commands.chat_emotes.export.success"),
                        true
                );
                return 1;
            }
        }
        context.getSource().sendFailure(fallbackTranslatable("commands.chat_emotes.export.failure.no_pack"));
        return 0;
    }

    public static @NotNull CompletableFuture<Suggestions> suggestResourcepackNames(
            final CommandContext<CommandSourceStack> context,
            final SuggestionsBuilder builder
    ) {
        final var config = ChatEmotesMod.getConfig();
        if (config != null) {
            for (ResourcepackConfig pack : config.getResourcepacks()) {
                builder.suggest(StringArgumentType.escapeIfRequired(pack.name()));
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    }

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        final var reload = literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(ChatEmotesCommand::tryReloadConfig);

        final var export = literal("export")
                .requires(source -> source.hasPermission(4))
                .then(
                        argument("pack name", StringArgumentType.string())
                                .suggests(ChatEmotesCommand::suggestResourcepackNames)
                                .executes(ChatEmotesCommand::tryExportResourcepacks)
                );

        dispatcher.register(
                literal(MOD_ID)
                        .then(reload)
                        .then(export)
        );
    }
}
