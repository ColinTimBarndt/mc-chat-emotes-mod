package io.github.colintimbarndt.chat_emotes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.colintimbarndt.chat_emotes.ChatEmotesMod;
import static net.minecraft.server.command.CommandManager.literal;

import static io.github.colintimbarndt.chat_emotes.TranslationHelper.translatable;
import net.minecraft.server.command.ServerCommandSource;

public class ChatEmotesCommand {
    private static int tryReloadConfig(CommandContext<ServerCommandSource> context) {
        final var source = context.getSource();
        if (ChatEmotesMod.reloadConfig()) {
            source.sendFeedback(
                    translatable("chat_emotes.commands.reload.success"),
                    true
            );
            return 1;
        } else {
            source.sendFeedback(
                    translatable("chat_emotes.commands.reload.failure"),
                    true
            );
            return 0;
        }
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("chat-emotes")
                        .then(
                                literal("reload")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(ChatEmotesCommand::tryReloadConfig)
                        )
        );
    }
}
