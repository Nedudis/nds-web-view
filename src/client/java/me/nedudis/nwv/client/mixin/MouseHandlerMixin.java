package me.nedudis.nwv.client.mixin;

import me.nedudis.nwv.client.browser.BrowserManager;
import me.nedudis.nwv.client.interaction.BrowserInteraction;
import net.dimaskama.mcef.api.MCEFBrowser;
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
        ci.cancel();
    }
}
