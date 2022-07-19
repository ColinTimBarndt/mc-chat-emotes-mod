package io.github.colintimbarndt.chat_emotes;

import io.github.colintimbarndt.chat_emotes.config.ChatEmotesConfig;
import io.github.colintimbarndt.chat_emotes.data.Emote;
import io.github.colintimbarndt.chat_emotes.data.EmoteData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

import static io.github.colintimbarndt.chat_emotes.TranslationHelper.fallback;

public final class EmoteDecorator {
    public static final ChatDecorator EMOTES =
            (sender, message) -> CompletableFuture.completedFuture(replaceEmotes(sender, message));

    private static final Style FONT_BASE_STYLE = Style.EMPTY.withColor(ChatFormatting.WHITE);
    private static final Style HIGHLIGHT_STYLE = Style.EMPTY.withColor(ChatFormatting.YELLOW);

    private static @Nullable Emote emoteForAlias(String alias) {
        final var dataList = ChatEmotesMod.EMOTE_DATA_LOADER.getLoadedEmoteData();
        for (EmoteData data : dataList) {
            final var result = data.emoteForAlias(alias);
            if (result != null) return result;
        }
        return null;
    }

    private static @Nullable Emote emoteForEmoticon(String emoticon) {
        final var dataList = ChatEmotesMod.EMOTE_DATA_LOADER.getLoadedEmoteData();
        for (EmoteData data : dataList) {
            final var result = data.emoteForEmoticon(emoticon);
            if (result != null) return result;
        }
        return null;
    }

    public static @NotNull Component createEmoteComponent(@NotNull Emote emote, String fallback) {
        return fallback(
                Component.literal(fallback),
                Component.literal(String.valueOf(emote.character()))
                        .setStyle(
                                FONT_BASE_STYLE.withFont(emote.font())
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(":" + emote.aliases()[0] + ":")
                                                        .setStyle(HIGHLIGHT_STYLE)
                                        ))
                        )
        );
    }

    public static @NotNull Component replaceEmotes(ServerPlayer sender, @NotNull Component comp) {
        final var content = comp.getContents();
        MutableComponent mut = null;
        if (content instanceof final LiteralContents literalContent) {
            final var text = literalContent.text();
            int startClip = 0;
            int startAlias = -1;
            int startEmoticon = 0;
            for (int i = 0; i < text.length(); i++) {
                final char ch = text.charAt(i);
                if (ch == ':') {
                    if (startAlias == -1 || startAlias == i) {
                        startAlias = i + 1;
                    } else {
                        final var alias = text.substring(startAlias, i);
                        final var emote = emoteForAlias(alias);
                        if (emote == null) {
                            startAlias = i + 1;
                        } else {
                            if (mut == null) mut = Component.empty();
                            mut.append(text.substring(startClip, startAlias - 1));
                            mut.append(createEmoteComponent(emote, ":" + alias + ":"));
                            startClip = i + 1;
                            startAlias = -1;
                        }
                    }
                    continue;
                }
                if (ch == ' ') {
                    startAlias = -1;
                    if (startEmoticon != i) {
                        final var emoticon = text.substring(startEmoticon, i);
                        final var emote = emoteForEmoticon(emoticon);
                        if (emote != null) {
                            if (mut == null) mut = Component.empty();
                            mut.append(text.substring(startClip, startEmoticon));
                            mut.append(createEmoteComponent(emote, emoticon));
                            startClip = i;
                        }
                    }
                    startEmoticon = i + 1;
                }
            }
            if (startEmoticon != text.length()) {
                final var emoticon = text.substring(startEmoticon);
                final var emote = emoteForEmoticon(emoticon);
                if (emote != null) {
                    if (mut == null) mut = Component.empty();
                    mut.append(text.substring(startClip, startEmoticon));
                    mut.append(createEmoteComponent(emote, emoticon));
                    startClip = text.length();
                }
            }
            if (mut == null) return comp;
            if (startClip != text.length()) {
                mut.append(text.substring(startClip));
            }
            comp.getSiblings().forEach(mut::append);
            return mut;
        }
        return comp;
    }
}
