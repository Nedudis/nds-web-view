package me.nedudis.nwv.client.mixin;

import me.nedudis.nwv.client.browser.BrowserAwtInput;
import me.nedudis.nwv.client.browser.BrowserInputState;
import me.nedudis.nwv.client.NDSWebViewClient;
import me.nedudis.nwv.client.browser.BrowserManager;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.cef.browser.CefBrowser;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void interceptBrowserKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (!BrowserInputState.typingMode) return;

        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return;
        }

        if (NDSWebViewClient.getTypingToggleKey() != null && NDSWebViewClient.getTypingToggleKey().matches(event)) {
            return;
        }

        MCEFBrowser browser = BrowserManager.getBrowser();
        if (browser == null) return;

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            browser.onKeyPressed(event);
        } else if (action == GLFW.GLFW_RELEASE) {
            browser.onKeyReleased(event);
        }
        ci.cancel();
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void interceptBrowserChar(long handle, CharacterEvent event, CallbackInfo ci) {
        if (!BrowserInputState.typingMode) return;

        MCEFBrowser browser = BrowserManager.getBrowser();
        if (browser == null) return;

        browser.onCharTyped(event);
        ci.cancel();
    }
}
