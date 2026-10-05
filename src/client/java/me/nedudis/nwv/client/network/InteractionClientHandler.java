package me.nedudis.nwv.client.network;

import me.nedudis.nwv.client.browser.BrowserInstance;
import me.nedudis.nwv.client.browser.BrowserManager;
import me.nedudis.nwv.network.ScreenInteractPayload;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class InteractionClientHandler {

    public static void handleInteract(ScreenInteractPayload payload, ClientPlayNetworking.Context context) {
        BrowserInstance instance = BrowserManager.getScreen(payload.screenName());
        if (instance == null || !instance.isReady()) return;

        MCEFBrowser browser = instance.getBrowser();
        if (browser == null) return;

        int pbWidth = (int) (instance.getData().widthBlocks() * 120);
        int pbHeight = (int) (instance.getData().heightBlocks() * 120);

        int realX = (int) (payload.x() * pbWidth);
        int realY = (int) (payload.y() * pbHeight);

        switch (payload.actionType()) {
            
            case ScreenInteractPayload.Actions.MOUSE_DOWN:
                browser.onMouseClicked(new net.minecraft.client.input.MouseButtonEvent(realX, realY, new net.minecraft.client.input.MouseButtonInfo(payload.buttonOrKey(), payload.modifiers())), false);
                break;
            case ScreenInteractPayload.Actions.MOUSE_UP:
                browser.onMouseReleased(new net.minecraft.client.input.MouseButtonEvent(realX, realY, new net.minecraft.client.input.MouseButtonInfo(payload.buttonOrKey(), payload.modifiers())));
                break;
            case ScreenInteractPayload.Actions.SCROLL:
                browser.onMouseScrolled(realX, realY, payload.y());
                break;

            case ScreenInteractPayload.Actions.KEY_DOWN:
                try {
                    java.awt.event.KeyEvent awtEvent = new java.awt.event.KeyEvent(
                        browser.getCefBrowser().getUIComponent(),
                        java.awt.event.KeyEvent.KEY_PRESSED,
                        System.currentTimeMillis(),
                        payload.modifiers(),
                        payload.buttonOrKey(),
                        java.awt.event.KeyEvent.CHAR_UNDEFINED
                    );
                    browser.getCefBrowser().getUIComponent().dispatchEvent(awtEvent);
                } catch (Exception e) {}
                break;
            case ScreenInteractPayload.Actions.KEY_UP:
                try {
                    java.awt.event.KeyEvent awtEvent = new java.awt.event.KeyEvent(
                        browser.getCefBrowser().getUIComponent(),
                        java.awt.event.KeyEvent.KEY_RELEASED,
                        System.currentTimeMillis(),
                        payload.modifiers(),
                        payload.buttonOrKey(),
                        java.awt.event.KeyEvent.CHAR_UNDEFINED
                    );
                    browser.getCefBrowser().getUIComponent().dispatchEvent(awtEvent);
                } catch (Exception e) {}
                break;
            case ScreenInteractPayload.Actions.CHAR_TYPED:
                try {
                    java.awt.event.KeyEvent awtEvent = new java.awt.event.KeyEvent(
                        browser.getCefBrowser().getUIComponent(),
                        java.awt.event.KeyEvent.KEY_TYPED,
                        System.currentTimeMillis(),
                        payload.modifiers(),
                        java.awt.event.KeyEvent.VK_UNDEFINED,
                        (char) payload.buttonOrKey()
                    );
                    browser.getCefBrowser().getUIComponent().dispatchEvent(awtEvent);
                } catch (Exception e) {}
                break;
        }
    }
}
