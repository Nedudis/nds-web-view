package me.nedudis.nwv.client.mixin;

import me.nedudis.nwv.client.browser.BrowserManager;
import me.nedudis.nwv.client.interaction.BrowserInteraction;
import me.nedudis.nwv.network.ScreenInteractPayload;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (buttonInfo.button() != 0 && buttonInfo.button() != 1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.player == null || mc.level == null) return;

        Player player = mc.player;
        Vec3 cameraPos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);

        Optional<BrowserInteraction.HitInfo> hit = BrowserInteraction.raycast(cameraPos, lookVec);
        if (hit.isEmpty()) {
            if (action == 1) BrowserManager.focusedScreen = null;
            return;
        }

        if (action == 1) BrowserManager.focusedScreen = hit.get().instance();

        MCEFBrowser browser = hit.get().instance().getBrowser();
        if (browser == null) return;

        int[] px = BrowserInteraction.toBrowserPixels(hit.get().instance(), hit.get().localX(), hit.get().localY());
        boolean pressed = action == 1;
        MouseButtonEvent event = new MouseButtonEvent(px[0], px[1], buttonInfo);

        if (pressed) browser.onMouseClicked(event, false);
        else browser.onMouseReleased(event);

        // SYNC INTERACTION TO SERVER
        double[] uv = BrowserInteraction.toNormalizedUV(hit.get().instance(), hit.get().localX(), hit.get().localY());
        int actionType = pressed ? ScreenInteractPayload.Actions.MOUSE_DOWN : ScreenInteractPayload.Actions.MOUSE_UP;
        
        if (ClientPlayNetworking.canSend(ScreenInteractPayload.TYPE)) {
            ClientPlayNetworking.send(new ScreenInteractPayload(
                hit.get().instance().getName(),
                actionType,
                uv[0], uv[1],
                buttonInfo.button(),
                buttonInfo.modifiers()
            ));
        }

        ci.cancel();
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void onMove(long handle, double xpos, double ypos, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.player == null || mc.level == null) return;

        Player player = mc.player;

        Vec3 cameraPos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Optional<BrowserInteraction.HitInfo> hit = BrowserInteraction.raycast(cameraPos, lookVec);
        if (hit.isEmpty()) return;

        MCEFBrowser browser = hit.get().instance().getBrowser();
        if (browser == null) return;

        int[] px = BrowserInteraction.toBrowserPixels(hit.get().instance(), hit.get().localX(), hit.get().localY());
        browser.onMouseMoved(px[0], px[1]);
        
        // We INTENTIONALLY do not sync mouse hovers to avoid severe server lag!
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.player == null || mc.level == null) return;

        Player player = mc.player;

        Vec3 cameraPos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Optional<BrowserInteraction.HitInfo> hit = BrowserInteraction.raycast(cameraPos, lookVec);
        if (hit.isEmpty()) return;

        MCEFBrowser browser = hit.get().instance().getBrowser();
        if (browser == null) return;

        int[] px = BrowserInteraction.toBrowserPixels(hit.get().instance(), hit.get().localX(), hit.get().localY());
        browser.onMouseScrolled(px[0], px[1], yoffset);

        // SYNC INTERACTION TO SERVER
        double[] uv = BrowserInteraction.toNormalizedUV(hit.get().instance(), hit.get().localX(), hit.get().localY());
        if (ClientPlayNetworking.canSend(ScreenInteractPayload.TYPE)) {
            ClientPlayNetworking.send(new ScreenInteractPayload(
                hit.get().instance().getName(),
                ScreenInteractPayload.Actions.SCROLL,
                uv[0], uv[1],
                (int) yoffset,
                0 // Scroll modifiers if we need
            ));
        }

        ci.cancel();
    }
}
