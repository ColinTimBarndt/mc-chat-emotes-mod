package io.github.colintimbarndt.chat_emotes;

import net.minecraft.network.message.MessageDecorator;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public final class EmoteDecorator {
    public static final MessageDecorator EMOTES = (sender, message) -> {
        final var config = ChatEmotesMod.getConfig();
        if (config == null) return CompletableFuture.completedFuture(message);
        return CompletableFuture.completedFuture(replaceEmotes(message, config));
    };

    public static @NotNull Text replaceEmotes(@NotNull Text text, @NotNull ChatEmotesConfig config) {
        final var content = text.getContent();
        MutableText mut = null;
        if (content instanceof LiteralTextContent) {
            final var lit = ((LiteralTextContent) content).string();
            final var words = lit.split(" ");
            final var emoteTypes = new ArrayList<ChatEmotesConfig.Emote>(0);
            final var emoteIdx = new HashSet<Integer>(0);
            // Triplets of (word index, begin, end)
            final var emoteOffsets = new ArrayList<Integer>();

            final var fontStyle = Style.EMPTY.withFont(
                    config.font()
            );
            final var highlightStyle = Style.EMPTY.withColor(Formatting.YELLOW);

            for (int i = 0; i < words.length; i++) {
                final String word = words[i];
                if (config.isSpecialEmote(word)) {
                    emoteTypes.add(config.getEmote(word));
                    emoteIdx.add(i);
                    emoteOffsets.add(i);
                    emoteOffsets.add(0);
                    emoteOffsets.add(word.length());
                    continue;
                }
                int col1 = 0, col2;
                while (true) {
                    col1 = word.indexOf(':', col1);
                    if (col1 < 0) break;
                    col2 = word.indexOf(':', col1 + 1);
                    if (col2 < 0) break;
                    final var emote = config.getEmote(word.substring(col1 + 1, col2));
                    if (emote != null) {
                        emoteTypes.add(emote);
                        emoteIdx.add(i);
                        emoteOffsets.add(i);
                        emoteOffsets.add(col1);
                        emoteOffsets.add(col2 + 1);
                    }
                    col1 = col2 + 1;
                }
            }
            if (!emoteIdx.isEmpty()) {
                int offIdx = 0;
                int emotyIdx = 0;
                mut = Text.literal("");
                mut.setStyle(text.getStyle());
                var buf = new StringBuilder();
                boolean space = false;
                for (int i = 0; i < words.length; i++, space = true) {
                    if (space) {
                        buf.append(' ');
                    }
                    if (!emoteIdx.contains(i)) {
                        buf.append(words[i]);
                    } else {
                        final var word = words[i];
                        int prevEnd = 0, begin, end = 0;
                        mut.append(buf.toString());
                        while (offIdx < emoteOffsets.size() && i == emoteOffsets.get(offIdx)) {
                            final var emote = emoteTypes.get(emotyIdx++);
                            offIdx++;
                            begin = emoteOffsets.get(offIdx++);
                            end = emoteOffsets.get(offIdx++);
                            mut.append(word.substring(prevEnd, begin));
                            final var raw = word.substring(begin, end);
                            mut.append(Text.translatable(
                                    "%1$s%784014$s",
                                    Text.literal(raw).setStyle(highlightStyle),
                                    Text.literal(emote.character()).setStyle(fontStyle)
                            ).setStyle(
                                    Style.EMPTY.withHoverEvent(HoverEvent.Action.SHOW_TEXT.buildHoverEvent(
                                            Text.literal(":" + emote.name() + ":")
                                                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
                                    ))
                            ));
                            prevEnd = end;
                        }
                        buf = new StringBuilder(word.substring(end));
                    }
                }
                if (!buf.isEmpty()) {
                    mut.append(buf.toString());
                }
                text.getSiblings().forEach(mut::append);
            }
        }
        return mut != null ? mut : text;
    }
}
