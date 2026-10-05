package me.nedudis.nwv.network;

import me.nedudis.nwv.screen.ScreenData;
import me.nedudis.nwv.screen.ScreenRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InteractionServerHandler {

    private static final Map<UUID, Long> lastInteractionTimes = new HashMap<>();
    private static final Map<UUID, Integer> interactionCounts = new HashMap<>();
    private static final long RATE_LIMIT_WINDOW_MS = 1000;
    private static final int MAX_INTERACTIONS_PER_SEC = 25; // Generous for scrolling/typing

    public static void handleInteract(ScreenInteractPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!checkRateLimit(player.getUUID())) return;

        ServerLevel level = (ServerLevel) player.level();
        ScreenRegistry registry = ScreenRegistry.get(level);
        Optional<ScreenData> screenOpt = registry.getScreen(payload.screenName());

        if (screenOpt.isEmpty() || !screenOpt.get().enabled()) return;

        ScreenData screen = screenOpt.get();

        // 1. Distance Calculation Fake Raycast (simplified distance & bounds)
        Vec3 center = new Vec3(screen.pos().getX() + 0.5, screen.pos().getY() + (screen.heightBlocks() / 2.0), screen.pos().getZ() + 0.5);
        if (player.position().distanceToSqr(center) > 100) { // ~10 blocks
            return;
        }

        // 2. Bounds Validation for UV Coordinates
        // Unless it's scrolling/key events, X and Y should be between 0.0 and 1.0
        if (payload.actionType() == ScreenInteractPayload.Actions.MOUSE_DOWN || payload.actionType() == ScreenInteractPayload.Actions.MOUSE_UP) {
            if (payload.x() < 0.0 || payload.x() > 1.0 || payload.y() < 0.0 || payload.y() > 1.0) {
                return;
            }
        }

        // Broadcast to players in tracking range, EXCEPT the sender
        for (ServerPlayer p : PlayerLookup.tracking(level, screen.pos())) {
            if (!p.getUUID().equals(player.getUUID())) {
                if (ServerPlayNetworking.canSend(p, ScreenInteractPayload.TYPE)) {
                    ServerPlayNetworking.send(p, payload);
                }
            }
        }
    }

    public static void handleUrlUpdate(ScreenUrlUpdatePayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        ServerLevel level = (ServerLevel) player.level();
        ScreenRegistry registry = ScreenRegistry.get(level);
        Optional<ScreenData> screenOpt = registry.getScreen(payload.screenName());

        if (screenOpt.isEmpty() || !screenOpt.get().enabled()) return;
        ScreenData screen = screenOpt.get();

        // Distance Check
        Vec3 center = new Vec3(screen.pos().getX() + 0.5, screen.pos().getY() + (screen.heightBlocks() / 2.0), screen.pos().getZ() + 0.5);
        if (player.position().distanceToSqr(center) > 100) return;

        // Security: Check if Domain is explicitly unallowed (e.g. localhost)
        String newUrl = payload.newUrl().toLowerCase();
        if (newUrl.contains("127.0.0.1") || newUrl.contains("localhost") || newUrl.startsWith("file://")) {
            return; 
            // In a full implementation, you'd load allowed domains from nwv_server.json
        }

        ScreenData updated = screen.withUrl(payload.newUrl());
        registry.addOrUpdateScreen(updated);

        // Tell EVERYONE (including the sender, just to securely align state) safely
        ScreenSyncPayload syncPayload = new ScreenSyncPayload(new java.util.ArrayList<>(registry.getScreens().values()));
        for (ServerPlayer p : PlayerLookup.all(level.getServer())) {
            if (ServerPlayNetworking.canSend(p, ScreenSyncPayload.TYPE)) {
                ServerPlayNetworking.send(p, syncPayload);
            }
        }
    }

    private static boolean checkRateLimit(UUID uuid) {
        long now = System.currentTimeMillis();
        long lastTime = lastInteractionTimes.getOrDefault(uuid, 0L);

        if (now - lastTime > RATE_LIMIT_WINDOW_MS) {
            lastInteractionTimes.put(uuid, now);
            interactionCounts.put(uuid, 1);
            return true;
        }

        int count = interactionCounts.getOrDefault(uuid, 0) + 1;
        interactionCounts.put(uuid, count);
        return count <= MAX_INTERACTIONS_PER_SEC;
    }
}
