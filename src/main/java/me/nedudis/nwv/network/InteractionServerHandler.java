package me.nedudis.nwv.network;

import me.nedudis.nwv.NWVServerConfig;
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

    public static void handleInteract(ScreenInteractPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!checkRateLimit(player.getUUID())) return;

        ServerLevel level = (ServerLevel) player.level();
        ScreenRegistry registry = ScreenRegistry.get(level);
        Optional<ScreenData> screenOpt = registry.getScreen(payload.screenName());

        if (screenOpt.isEmpty() || !screenOpt.get().enabled()) return;

        ScreenData screen = screenOpt.get();

        Vec3 center = new Vec3(screen.pos().getX() + 0.5, screen.pos().getY() + (screen.heightBlocks() / 2.0), screen.pos().getZ() + 0.5);
        double maxDistSq = NWVServerConfig.get().maxInteractDistance * NWVServerConfig.get().maxInteractDistance;
        if (player.position().distanceToSqr(center) > maxDistSq) {
            return;
        }

        if (payload.actionType() == ScreenInteractPayload.Actions.MOUSE_DOWN || payload.actionType() == ScreenInteractPayload.Actions.MOUSE_UP) {
            if (payload.x() < 0.0 || payload.x() > 1.0 || payload.y() < 0.0 || payload.y() > 1.0) {
                return;
            }
        }

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

        Vec3 center = new Vec3(screen.pos().getX() + 0.5, screen.pos().getY() + (screen.heightBlocks() / 2.0), screen.pos().getZ() + 0.5);
        double maxDistSq = NWVServerConfig.get().maxInteractDistance * NWVServerConfig.get().maxInteractDistance;
        if (player.position().distanceToSqr(center) > maxDistSq) return;

        if (!NWVServerConfig.isUrlAllowed(payload.newUrl())) {
            return; 
        }

        ScreenData updated = screen.withUrl(payload.newUrl());
        registry.addOrUpdateScreen(updated);

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
        return count <= NWVServerConfig.get().maxInteractionsPerSecond;
    }
}
