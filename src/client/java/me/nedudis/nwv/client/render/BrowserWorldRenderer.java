package me.nedudis.nwv.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.nedudis.nwv.client.browser.BrowserInstance;
import me.nedudis.nwv.client.browser.BrowserManager;
import me.nedudis.nwv.screen.ScreenData;
import net.minecraft.client.renderer.SubmitNodeCollector;

public class BrowserWorldRenderer {

    public static void renderInWorld(PoseStack poseStack, SubmitNodeCollector nodeCollector, float cameraX, float cameraY, float cameraZ) {
        for(BrowserInstance instance : BrowserManager.getAllScreens()) {
            if (instance == null || !instance.isReady() || instance.getData() == null || !instance.getData().enabled()) continue;

            poseStack.pushPose();
            ScreenData data = instance.getData();

            poseStack.translate(
                    data.pos().getX() + 0.5 - cameraX,
                    data.pos().getY() - cameraY,
                    data.pos().getZ() + 0.5 - cameraZ
            );

            if (data.facing() != null) {
                float rot = data.facing().toYRot();
                poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            }

            nodeCollector.submitCustomGeometry(poseStack, instance.getRenderType(),
                    (pose, buffer) -> drawQuad(pose, buffer, data));
            poseStack.popPose();
        }
    }

    public static void drawQuad(PoseStack.Pose pose, VertexConsumer buffer, ScreenData data) {
        float w = data.widthBlocks();
        float h = data.heightBlocks();

        // Bottom left
        buffer.addVertex(pose, 0.0f, 0.0f, 0.0f).setColor(-1).setUv(0.0f, 1.0f).setLight(15728880);
        // Bottom right
        buffer.addVertex(pose, w, 0.0f, 0.0f).setColor(-1).setUv(1.0f, 1.0f).setLight(15728880);
        // Top right
        buffer.addVertex(pose, w, h, 0.0f).setColor(-1).setUv(1.0f, 0.0f).setLight(15728880);
        // Top left
        buffer.addVertex(pose, 0.0f, h, 0.0f).setColor(-1).setUv(0.0f, 0.0f).setLight(15728880);
    }
}
