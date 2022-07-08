package io.github.colintimbarndt.chat_emotes;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.CommandDispatcher;
import io.github.colintimbarndt.chat_emotes.command.ChatEmotesCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ChatEmotesMod implements DedicatedServerModInitializer, ClientModInitializer {
	public static final String MODID = "chat_emotes";
	public static final Logger LOGGER = LoggerFactory.getLogger("chat-emotes");

	private static ChatEmotesConfig config = null;

	@Override
	public void onInitializeServer() {
		reloadConfig();
		CommandRegistrationCallback.EVENT.register(this::onRegisterCommands);
	}

	@Override
	public void onInitializeClient() {
		//
	}

	private void onRegisterCommands(
			CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess reg,
			CommandManager.RegistrationEnvironment env
	) {
		if (env.dedicated) {
			ChatEmotesCommand.register(dispatcher);
		}
	}

	public static @Nullable ChatEmotesConfig getConfig() {
		return config;
	}

	public static boolean reloadConfig() {
		try {
			config = ChatEmotesConfig.load();
			LOGGER.info("Loaded %d emotes".formatted(config.emotes()));
			return true;
		} catch (IOException | JsonParseException e) {
			config = ChatEmotesConfig.DEFAULT;
			LOGGER.error("Unable to read config", e);
			return false;
		}
	}
}
