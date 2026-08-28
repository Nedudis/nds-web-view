package me.nedudis.nwv.client.interaction;

import me.nedudis.nwv.client.browser.BrowserInstance;
import me.nedudis.nwv.client.browser.BrowserManager;
import me.nedudis.nwv.screen.ScreenData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BrowserInteraction {
    private static final double REACH_DISTANCE = 6.0;

    public record HitInfo(BrowserInstance instance, double localX, double localY) {}

    public static Optional<HitInfo> raycast(Vec3 cameraPos, Vec3 lookVec) {
        BrowserInstance closestInstance = null;
        double closestT = REACH_DISTANCE;
        double closestLocalX = 0;
        double closestLocalY = 0;

        for(BrowserInstance instance : BrowserManager.getAllScreens()) {
            if (!instance.isReady() || !instance.getData().enabled()) continue;

            ScreenData data = instance.getData();
            Direction facing = data.facing() != null ? data.facing() : Direction.SOUTH;

            double originX = data.pos().getX() + 0.5;
            double originY = data.pos().getY();
            double originZ = data.pos().getZ() + 0.5;

            Vec3 localCam = cameraPos.subtract(originX, originY, originZ);

            float rot = facing.toYRot();
            double rad = Math.toRadians(rot);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            double camX = localCam.x * cos - localCam.z * sin;
            double camY = localCam.y;
            double camZ = localCam.x * sin + localCam.z * cos;

            double dirX = lookVec.x * cos - lookVec.z * sin;
            double dirY = lookVec.y;
            double dirZ = lookVec.x * sin + lookVec.z * cos;

            if (Math.abs(dirZ) < 1e-6) continue;

            double t = (0 - camZ) / dirZ;
            if (t < 0 || t > closestT) continue;

            double localX = camX + dirX * t;
            double localY = camY + dirY * t;

            if (localX < 0.0 || localX > data.widthBlocks() ||
                localY < 0.0 || localY > data.heightBlocks()) {
                continue;
            }

            Level level = Minecraft.getInstance().level;
            if (level == null) continue;

            double hitWorldX = cameraPos.x + lookVec.x * t;
            double hitWorldY = cameraPos.y + lookVec.y * t;
            double hitWorldZ = cameraPos.z + lookVec.z * t;
            Vec3 hitPoint = new Vec3(hitWorldX, hitWorldY, hitWorldZ);

            BlockHitResult clip = level.clip(new ClipContext(
                    cameraPos, hitPoint,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    Minecraft.getInstance().player
            ));

            if (clip.getType() != HitResult.Type.MISS) continue;

            closestT = t;
            closestInstance = instance;
            closestLocalX = localX;
            closestLocalY = localY;
        }

        if (closestInstance == null) return Optional.empty();

        return Optional.of(new HitInfo(closestInstance, closestLocalX, closestLocalY));
    }

    public static int[] toBrowserPixels(BrowserInstance instance, double localX, double localY) {
        ScreenData data = instance.getData();

        double fracX = localX / data.widthBlocks();
        double fracY = localY / data.heightBlocks();

        int pxWidth = (int) (data.widthBlocks() * 120);
        int pxHeight = (int) (data.heightBlocks() * 120);

        int px = (int) (fracX * pxWidth);
        int py = (int) ((1.0 - fracY) * pxHeight);
        return new int[] {px, py};
    }
}
