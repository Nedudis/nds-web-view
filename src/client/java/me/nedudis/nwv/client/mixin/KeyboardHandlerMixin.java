package me.nedudis.nwv.client.mixin;

import me.nedudis.nwv.client.browser.BrowserInputState;
import me.nedudis.nwv.client.NDSWebViewClient;
import me.nedudis.nwv.client.browser.BrowserManager;
import me.nedudis.nwv.network.ScreenInteractPayload;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    private void sendKeyPayload(int action, int payloadVal, int modifiers) {
        if (BrowserManager.focusedScreen == null) return;
        ScreenInteractPayload payload = new ScreenInteractPayload(
            BrowserManager.focusedScreen.getName(),
            action,
            0.0, 0.0,
            payloadVal, modifiers
        );
        if (ClientPlayNetworking.canSend(ScreenInteractPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void interceptBrowserKey(long handle, int unknownInt, KeyEvent event, CallbackInfo ci) {
        if (!BrowserInputState.typingMode || BrowserManager.focusedScreen == null) return;

        int key = event.key();
        int action = event.input();
        int modifiers = event.modifiers();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            BrowserInputState.typingMode = false;
            MCEFBrowser browser = BrowserManager.focusedScreen.getBrowser();
            if (browser != null) browser.setFocus(false);
            ci.cancel(); 
            return;
        }

        if (NDSWebViewClient.getTypingToggleKey() != null && NDSWebViewClient.getTypingToggleKey().matches(InputConstants.Type.KEYSYM.getOrCreate(key))) {
            return; // let vanilla process toggle so it can turn OFF!
        }

        MCEFBrowser browser = BrowserManager.focusedScreen.getBrowser();
        if (browser == null) return;

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            browser.onKeyPressed(event);
            sendKeyPayload(ScreenInteractPayload.Actions.KEY_DOWN, key, modifiers);
        } else if (action == GLFW.GLFW_RELEASE) {
            browser.onKeyReleased(event);
            sendKeyPayload(ScreenInteractPayload.Actions.KEY_UP, key, modifiers);
        }
        ci.cancel();
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void interceptBrowserChar(long handle, CharacterEvent event, CallbackInfo ci) {
        if (!BrowserInputState.typingMode || BrowserManager.focusedScreen == null) return;

        MCEFBrowser browser = BrowserManager.focusedScreen.getBrowser();
        if (browser == null) return;
        
        browser.onCharTyped(event);
        sendKeyPayload(ScreenInteractPayload.Actions.CHAR_TYPED, event.codepoint(), 0);
        ci.cancel();
    }
}
