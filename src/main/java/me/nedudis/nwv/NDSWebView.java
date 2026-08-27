package me.nedudis.nwv;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.nedudis.nwv.network.ScreenSyncPayload;
import me.nedudis.nwv.screen.ScreenData;
import me.nedudis.nwv.screen.ScreenRegistry;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class NDSWebView implements ModInitializer {
	public static final String MOD_ID = "nwv";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		PayloadTypeRegistry.clientboundPlay().register(ScreenSyncPayload.TYPE, ScreenSyncPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerLevel level = server.overworld();
			ScreenRegistry registry = ScreenRegistry.get(level);
			ServerPlayNetworking.send(handler.player, ScreenSyncPayload.of(registry.getScreen().orElse(null)));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			dispatcher.register(
				Commands.literal("nwv")
					.then(Commands.literal("seturl")
						.then(Commands.argument("url", StringArgumentType.greedyString())
							.executes(context -> {
								String url = StringArgumentType.getString(context, "url");
								ServerLevel level = context.getSource().getLevel();
								ScreenRegistry registry = ScreenRegistry.get(level);

								ScreenData newData = new ScreenData(
										context.getSource().getPlayerOrException().blockPosition(),
										Direction.NORTH, url, true, 16.0f, 9.0f
								);
								registry.setScreen(newData);

								ScreenSyncPayload payload = ScreenSyncPayload.of(newData);
								for (ServerPlayer p : PlayerLookup.all(context.getSource().getServer())) {
									ServerPlayNetworking.send(p, payload);
								}

								context.getSource().sendSuccess(() -> Component.literal("§a[NWV] URL set to everyone: " + url), true);
								return 1;
							})
						)
					)
					.then(Commands.literal("toggle")
						.executes(context -> {
							ServerLevel level = context.getSource().getLevel();
							ScreenRegistry registry = ScreenRegistry.get(level);

							if (registry.getScreen().isEmpty()) {
								context.getSource().sendFailure(Component.literal("§c[NWV] There are no active screens in this world. Firstly use /nwv seturl"));
								return 0;
							}

							ScreenData currentData = registry.getScreen().get();
							ScreenData toggled = currentData.withEnabled(!currentData.enabled());
							registry.setScreen(toggled);

							ScreenSyncPayload payload = ScreenSyncPayload.of(toggled);
							for(ServerPlayer p : PlayerLookup.all(context.getSource().getServer())) {
								ServerPlayNetworking.send(p, payload);
							}

							String stateStr = toggled.enabled() ? "§aON" : "§cOFF";
							context.getSource().sendSuccess(() -> Component.literal("[NWV] Screen is now: " + stateStr), true);
							return 1;
						})
					)
			);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
