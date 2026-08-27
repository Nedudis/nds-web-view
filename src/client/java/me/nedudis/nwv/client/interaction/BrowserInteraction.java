package me.nedudis.nwv.client.interaction;

import me.nedudis.nwv.client.browser.BrowserManager;
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

    public record HitInfo(double localX, double localY) {}

    public static Optional<HitInfo> raycast(Vec3 cameraPos, Vec3 lookVec) {
        Direction facing = BrowserManager.getFacing();
        if (facing == null) facing = Direction.SOUTH;

        Vec3 localCam = cameraPos.subtract(BrowserManager.getOriginX(), BrowserManager.getOriginY(), BrowserManager.getOriginZ());

        float rot = facing.toYRot();
        double rad = Math.toRadians(-rot);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double camX = localCam.x * cos - localCam.z * sin;
        double camY = localCam.y;
        double camZ = localCam.x * sin + localCam.z * cos;

        double dirX = lookVec.x * cos - lookVec.z * sin;
        double dirY = lookVec.y;
        double dirZ = lookVec.x * sin + lookVec.z * cos;

        if (Math.abs(dirZ) < 1e-6) return Optional.empty();

        double t = (0 - camZ) / dirZ;
        if (t < 0 || t > REACH_DISTANCE) return Optional.empty();

        double localX = camX + dirX * t;
        double localY = camY + dirY * t;

        if (localX < 0.0 || localX > BrowserManager.BROWSER_WIDTH_BLOCKS
                || localY < 0.0 || localY > BrowserManager.BROWSER_HEIGHT_BLOCKS) {
            return Optional.empty();
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();

        double hitWorldX = cameraPos.x + lookVec.x * t;
        double hitWorldY = cameraPos.y + lookVec.y * t;
        double hitWorldZ = cameraPos.z + lookVec.z * t;
        Vec3 hitPoint = new Vec3(hitWorldX, hitWorldY, hitWorldZ);

        BlockHitResult clip = level.clip(new ClipContext(
            cameraPos, hitPoint,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
            Minecraft.getInstance().player
        ));

        if (clip.getType() != HitResult.Type.MISS) return Optional.empty();

        return Optional.of(new HitInfo(localX, localY));
    }

    public static int[] toBrowserPixels(double localX, double localY) {
        double fracX = localX / BrowserManager.BROWSER_WIDTH_BLOCKS;
        double fracY = localY / BrowserManager.BROWSER_HEIGHT_BLOCKS;

        int px = (int) (fracX * BrowserManager.BROWSER_WIDTH);
        int py = (int) ((1.0 - fracY) * BrowserManager.BROWSER_HEIGHT);
        return new int[] {px, py};
    }
}
