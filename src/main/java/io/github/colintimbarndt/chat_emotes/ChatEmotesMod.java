package io.github.colintimbarndt.chat_emotes;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ChatEmotesMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("chat-emotes");

	private static ChatEmotesConfig config = null;

	@Override
	public void onInitialize() {
		try {
			config = ChatEmotesConfig.load();
		} catch (IOException | JsonParseException e) {
			config = ChatEmotesConfig.DEFAULT;
			LOGGER.error("Unable to read config", e);
		}
		LOGGER.info("Loaded %d emotes".formatted(config.emotes()));
	}

	public static @Nullable ChatEmotesConfig getConfig() {
		return config;
	}
}
