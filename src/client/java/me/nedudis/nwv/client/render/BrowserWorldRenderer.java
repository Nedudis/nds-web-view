package me.nedudis.nwv.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.nedudis.nwv.client.browser.BrowserManager;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;

public class BrowserWorldRenderer {

    public static void renderInWorld(PoseStack poseStack, SubmitNodeCollector nodeCollector, float cameraX, float cameraY, float cameraZ) {
        BrowserManager.init();
        if (!BrowserManager.isReady()) return;

        poseStack.pushPose();
        poseStack.translate(
                BrowserManager.getOriginX() - cameraX,
                BrowserManager.getOriginY() - cameraY,
                BrowserManager.getOriginZ() - cameraZ
        );


        Direction facing = BrowserManager.getFacing();
        if (facing != null) {
            float rot = facing.toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        }

        nodeCollector.submitCustomGeometry(poseStack, BrowserManager.getRenderType(), BrowserWorldRenderer::drawQuad);
        poseStack.popPose();
    }

    public static void drawQuad(PoseStack.Pose pose, VertexConsumer buffer) {
        // Bottom left
        buffer.addVertex(pose, 0.0f, 0.0f, 0.0f).setColor(-1).setUv(0.0f, 1.0f).setLight(15728880);
        // Bottom right
        buffer.addVertex(pose, BrowserManager.BROWSER_WIDTH_BLOCKS, 0.0f, 0.0f).setColor(-1).setUv(1.0f, 1.0f).setLight(15728880);
        // Top right
        buffer.addVertex(pose, BrowserManager.BROWSER_WIDTH_BLOCKS, BrowserManager.BROWSER_HEIGHT_BLOCKS, 0.0f).setColor(-1).setUv(1.0f, 0.0f).setLight(15728880);
        // Top left
        buffer.addVertex(pose, 0.0f, BrowserManager.BROWSER_HEIGHT_BLOCKS, 0.0f).setColor(-1).setUv(0.0f, 0.0f).setLight(15728880);
    }
}
