package io.github.colintimbarndt.chat_emotes;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Lifecycle;
import io.github.colintimbarndt.chat_emotes.commands.ChatEmotesCommand;
import io.github.colintimbarndt.chat_emotes.config.ChatEmotesConfig;
import io.github.colintimbarndt.chat_emotes.data.EmoteDataLoader;
import io.github.colintimbarndt.chat_emotes.data.EmoteDataSerializer;
import io.github.colintimbarndt.chat_emotes.data.unicode.UnicodeEmoteData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static net.minecraft.resources.ResourceKey.createRegistryKey;

public class ChatEmotesMod implements DedicatedServerModInitializer, ClientModInitializer {
	public static final String MOD_ID = "chat_emotes";
	public static final Logger LOGGER = LoggerFactory.getLogger("Chat Emotes");
	private static ChatEmotesConfig config = null;
	private static Path configDir = null;
	private static ModMetadata modMetadata = null;
	public static final ResourceKey<Registry<EmoteDataSerializer<?>>> EMOTE_DATA_SERIALIZER_REGISTRY;
	public static final Registry<EmoteDataSerializer<?>> EMOTE_DATA_SERIALIZER;
	public static final EmoteDataLoader EMOTE_DATA_LOADER = new EmoteDataLoader();

	static {
		EMOTE_DATA_SERIALIZER_REGISTRY = createRegistryKey(new ResourceLocation(MOD_ID, "emote_data_serializer"));
		EMOTE_DATA_SERIALIZER = new MappedRegistry<>(EMOTE_DATA_SERIALIZER_REGISTRY, Lifecycle.experimental(), null);
		Registry.register(EMOTE_DATA_SERIALIZER, new ResourceLocation(MOD_ID, "unicode"), UnicodeEmoteData.SERIALIZER);
	}

	public static ModMetadata getModMetadata() {
		return modMetadata;
	}

	private void onInitialize(FabricLoader loader) {
		modMetadata = loader.getModContainer(MOD_ID).orElseThrow().getMetadata();
	}

	@Override
	public void onInitializeServer() {
		final var loader = FabricLoader.getInstance();
		onInitialize(loader);
		configDir = loader.getConfigDir().resolve(MOD_ID);
		final var configDirFile = configDir.toFile();
		if (!(configDirFile.isDirectory() || configDirFile.mkdirs())) {
			LOGGER.error("Failed to create config directory");
			config = new ChatEmotesConfig();
		} else {
			reloadConfig();
		}
		CommandRegistrationCallback.EVENT.register(this::onRegisterCommands);
		ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, EmoteDecorator.EMOTES);
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(EMOTE_DATA_LOADER);
	}

	@Override
	public void onInitializeClient() {
		final var loader = FabricLoader.getInstance();
		onInitialize(loader);
		// TODO: implement client
	}

	private void onRegisterCommands(
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext reg,
			Commands.CommandSelection env
	) {
		if (env.includeDedicated) {
			ChatEmotesCommand.register(dispatcher);
		}
	}

	public static @Nullable ChatEmotesConfig getConfig() {
		return config;
	}

	public static boolean reloadConfig() {
		final var file = configDir.resolve("config.json").toFile();
		if (!file.exists()) {
			config = new ChatEmotesConfig();
			try {
				if (!file.createNewFile()) {
					LOGGER.error("Unable to create default config file");
					return false;
				}
				config.save(new FileOutputStream(file));
				return true;
			} catch (IOException ex) {
				LOGGER.error("Unable to write default config", ex);
				return false;
			}
		}
		try {
			config = ChatEmotesConfig.load(new FileInputStream(file));
			LOGGER.info("Loaded config");
			return true;
		} catch (IOException | JsonParseException ex) {
			config = new ChatEmotesConfig();
			LOGGER.error("Unable to read config", ex);
			return false;
		}
	}

	public static Path getConfigDir() {
		return configDir;
	}
}
