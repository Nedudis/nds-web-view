package me.nedudis.nwv;

import com.mojang.brigadier.arguments.FloatArgumentType;
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

import java.util.ArrayList;
import java.util.Optional;

public class NDSWebView implements ModInitializer {
	public static final String MOD_ID = "nwv";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		PayloadTypeRegistry.clientboundPlay().register(ScreenSyncPayload.TYPE, ScreenSyncPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, ScreenSyncPayload.TYPE)) {
				ServerLevel level = server.overworld();
				ScreenRegistry registry = ScreenRegistry.get(level);
				ServerPlayNetworking.send(handler.player, new ScreenSyncPayload(new ArrayList<>(registry.getScreens().values())));
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			dispatcher.register(
				Commands.literal("nwv")
					.then(Commands.literal("create")
						.then(Commands.argument("name", StringArgumentType.word())
							.then(Commands.argument("width", FloatArgumentType.floatArg(1.0f, 100.0f))
								.then(Commands.argument("height", FloatArgumentType.floatArg(1.0f, 100.0f))
									.then(Commands.argument("url", StringArgumentType.greedyString())
										.executes(context -> {

											String name = StringArgumentType.getString(context, "name");
											float width = FloatArgumentType.getFloat(context, "width");
											float height = FloatArgumentType.getFloat(context, "height");
											String url = StringArgumentType.getString(context, "url");

											ServerPlayer player = context.getSource().getPlayerOrException();
											ServerLevel level = context.getSource().getLevel();
											ScreenRegistry registry = ScreenRegistry.get(level);

											Direction facing = player.getDirection().getOpposite();

											ScreenData newData = new ScreenData(
												name, player.blockPosition(), facing, url, true, width, height
											);

											registry.addOrUpdateScreen(newData);

											ScreenSyncPayload payload = new ScreenSyncPayload(new ArrayList<>(registry.getScreens().values()));
											for (ServerPlayer p : PlayerLookup.all(context.getSource().getServer())) {
												if (ServerPlayNetworking.canSend(p, ScreenSyncPayload.TYPE)) {
													ServerPlayNetworking.send(p, payload);
												}
											}

											context.getSource().sendSuccess(() -> Component.literal("§a[NWV] Screen '" + name + "' has been created."), true);
											return 1;
										})
									)
								)
							)
						)
					)
					.then(Commands.literal("delete")
						.then(Commands.argument("name", StringArgumentType.word())
							.executes(context -> {

								String name = StringArgumentType.getString(context, "name");

								ServerLevel level = context.getSource().getLevel();
								ScreenRegistry registry = ScreenRegistry.get(level);

								Optional<ScreenData> opt = registry.getScreen(name);
								if (opt.isEmpty()) {
									context.getSource().sendFailure(Component.literal("§c[NWV] The screen '" + name + "' was not found."));
									return 0;
								}

								registry.removeScreen(name);

								ScreenSyncPayload payload = new ScreenSyncPayload(new ArrayList<>(registry.getScreens().values()));

								for(ServerPlayer p : PlayerLookup.all(context.getSource().getServer())) {
									if (ServerPlayNetworking.canSend(p, ScreenSyncPayload.TYPE))
										ServerPlayNetworking.send(p, payload);
								}

								context.getSource().sendSuccess(() -> Component.literal("§a[NWV] Screen '" + name + "' has been permanently deleted."), true);
								return 1;
							})
						)
					)
					.then(Commands.literal("seturl")
						.then(Commands.argument("name", StringArgumentType.word())
							.then(Commands.argument("url", StringArgumentType.greedyString())
								.executes(context -> {

									String name = StringArgumentType.getString(context, "name");
									String url = StringArgumentType.getString(context, "url");

									ServerLevel level = context.getSource().getLevel();
									ScreenRegistry registry = ScreenRegistry.get(level);

									Optional<ScreenData> opt = registry.getScreen(name);
									if (opt.isEmpty()) {
										context.getSource().sendFailure(Component.literal("§c[NWV] The screen '" + name + "' was not found."));
										return 0;
									}

									ScreenData newData = opt.get().withUrl(url);
									registry.addOrUpdateScreen(newData);

									ScreenSyncPayload payload = new ScreenSyncPayload(new ArrayList<>(registry.getScreens().values()));
									for(ServerPlayer p : PlayerLookup.all(context.getSource().getServer())) {
										if (ServerPlayNetworking.canSend(p, ScreenSyncPayload.TYPE))
											ServerPlayNetworking.send(p, payload);
									}

									context.getSource().sendSuccess(() -> Component.literal("§a[NWV] Screen's '" + name + "' URL has been set to: " + url), true);
									return 1;
								})
							)
						)
					)
					.then(Commands.literal("toggle")
						.then(Commands.argument("name", StringArgumentType.word())
							.executes(context -> {

								String name = StringArgumentType.getString(context, "name");

								ServerLevel level = context.getSource().getLevel();
								ScreenRegistry registry = ScreenRegistry.get(level);

								Optional<ScreenData> opt = registry.getScreen(name);
								if (opt.isEmpty()) {
									context.getSource().sendFailure(Component.literal("§c[NWV] The screen '" + name + "' was not found."));
									return 0;
								}

								ScreenData toggled = opt.get().withEnabled(!opt.get().enabled());
								registry.addOrUpdateScreen(toggled);

								ScreenSyncPayload payload = new ScreenSyncPayload(new ArrayList<>(registry.getScreens().values()));
								for(ServerPlayer p : PlayerLookup.all(context.getSource().getServer())) {
									if (ServerPlayNetworking.canSend(p, ScreenSyncPayload.TYPE))
										ServerPlayNetworking.send(p, payload);
								}

								String stateStr = toggled.enabled() ? "§aON" : "§cOFF";
								context.getSource().sendSuccess(() -> Component.literal("§a[NWV] Screen '" + name + "' is now: " + stateStr), true);
								return 1;
							})
						)
					)
			);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
