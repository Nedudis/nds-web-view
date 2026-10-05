package me.nedudis.nwv.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.nedudis.nwv.client.browser.BrowserInputState;
import me.nedudis.nwv.client.browser.BrowserManager;
import me.nedudis.nwv.client.network.InteractionClientHandler;
import me.nedudis.nwv.client.render.BrowserWorldRenderer;
import me.nedudis.nwv.network.ScreenInteractPayload;
import me.nedudis.nwv.network.ScreenSyncPayload;
import net.dimaskama.mcef.api.MCEFApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;

public class NDSWebViewClient implements ClientModInitializer {

	static {
		System.setProperty("java.awt.headless", "false");
		try {
			java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
			System.out.println("[ NWV ] AWT Environment successfully forced.");
		} catch (Throwable t) {
			System.err.println("[ NWV ] AWT Environment failed: " + t.getMessage());
		}
	}

	private static KeyMapping typingToggleKey;
	private static KeyMapping backKey;
	private static KeyMapping forwardKey;
	private static KeyMapping homeKey;

	public static final KeyMapping.Category NWV_CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("nwv", "general"));

	@Override
	public void onInitializeClient() {
		System.out.println(">>> NWV: CLIENT INITIALIZATION STARTED <<<");

		NWVClientConfig.load();

		if (NWVClientConfig.get().incognitoMode) {
			System.out.println("[ NWV ] Incognito Mode Active: Wiping MCEF cache directory...");
			File mcefCache = new File(FabricLoader.getInstance().getConfigDir().toFile(), "mcef-modern/cache");
			if (mcefCache.exists()) deleteDirectory(mcefCache);
		}

		MCEFApi.initialize();

		typingToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.nwv.toggle_typing",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F,
				NWV_CATEGORY
		));

		backKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.nwv.back", InputConstants.Type.MOUSE,
				GLFW.GLFW_MOUSE_BUTTON_4,
				NWV_CATEGORY
		));

		forwardKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.nwv.forward", InputConstants.Type.MOUSE,
				GLFW.GLFW_MOUSE_BUTTON_5,
				NWV_CATEGORY
		));

		homeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.nwv.home", InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_HOME,
				NWV_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(typingToggleKey.consumeClick()) {
				BrowserInputState.typingMode = !BrowserInputState.typingMode;
				String status = BrowserInputState.typingMode ? "\u00a7aON" : "\u00a7cOFF";
				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal("[ NWV ] Typing mode: " + status));
				}
			}

			while (backKey.consumeClick()) BrowserManager.goBack();
			while (forwardKey.consumeClick()) BrowserManager.goForward();
			while (homeKey.consumeClick()) BrowserManager.loadDefaultUrl();

			if (client.player != null) BrowserManager.updateVolumeForAll(client.player);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			client.execute(() -> {
				BrowserManager.clearAllScreens();
				System.out.println("[ NWV ] Disconnected from the world/server. All screens have been deleted from the memory.");
			});
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommands.literal("nwvclient")
							.then(ClientCommands.literal("test")
									.executes(context -> {
										return 1;
									})
							)
			);
		});

		ClientPlayNetworking.registerGlobalReceiver(ScreenSyncPayload.TYPE, ((payload, context) -> {
			context.client().execute(() -> {
				BrowserManager.applySync(payload.screens());
			});
		}));

		ClientPlayNetworking.registerGlobalReceiver(ScreenInteractPayload.TYPE, ((payload, context) -> {
			context.client().execute(() -> {
				InteractionClientHandler.handleInteract(payload, context);
			});
		}));

		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
			var camera = context.levelState().cameraRenderState.pos;

			BrowserWorldRenderer.renderInWorld(
					context.poseStack(),
					context.submitNodeCollector(),
					(float) camera.x,
					(float) camera.y,
					(float) camera.z
			);
		});
	}

	public static KeyMapping getTypingToggleKey() { return typingToggleKey; }

	private boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}
}
