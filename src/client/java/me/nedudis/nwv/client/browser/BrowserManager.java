package me.nedudis.nwv.client.browser;

import me.nedudis.nwv.screen.ScreenData;
import net.minecraft.client.player.LocalPlayer;
import org.cef.browser.CefBrowser;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowserManager {

    private static final Map<String, BrowserInstance> activeScreens = new HashMap<>();

    public static BrowserInstance focusedScreen = null;


    public static BrowserInstance getScreen(String name) { return activeScreens.get(name); }
    public static Collection<BrowserInstance> getAllScreens() { return activeScreens.values(); }

    public static void applySync(List<ScreenData> screens) {
        List<String> incomingNames = screens.stream().map(ScreenData::name).toList();

        activeScreens.entrySet().removeIf(entry -> {
            if (!incomingNames.contains(entry.getKey())) {
                entry.getValue().close();
                if (focusedScreen == entry.getValue()) focusedScreen = null;
                return true;
            }
            return false;
        });

        for (ScreenData data : screens) {
            if (activeScreens.containsKey(data.name())) {
                activeScreens.get(data.name()).updateData(data);
            } else {
                activeScreens.put(data.name(), new BrowserInstance(data));
            }
        }
    }

    public static void updateVolumeForAll(LocalPlayer player) {
        for(BrowserInstance instance : activeScreens.values()) {
            instance.updateVolume(player);
        }
    }

    public static void clearAllScreens() {
        for(BrowserInstance instance : activeScreens.values()) {
            instance.close();
        }
        activeScreens.clear();
        focusedScreen = null;
    }

    public static void loadDefaultUrl() {
        if (focusedScreen == null || focusedScreen.getBrowser() == null) return;
        CefBrowser cefBrowser = focusedScreen.getBrowser().getCefBrowser();
        if (cefBrowser == null) return;

        cefBrowser.loadURL(BrowserInstance.DEFAULT_URL);
    }

    public static void goForward() {
        if (focusedScreen == null || focusedScreen.getBrowser() == null) return;

        CefBrowser cefBrowser = focusedScreen.getBrowser().getCefBrowser();
        if (cefBrowser == null) return;

        if (cefBrowser.canGoForward())
            cefBrowser.goForward();

    }

    public static void goBack() {
        if (focusedScreen == null || focusedScreen.getBrowser() == null) return;

        CefBrowser cefBrowser = focusedScreen.getBrowser().getCefBrowser();
        if (cefBrowser == null) return;

        if (cefBrowser.canGoBack())
            cefBrowser.goBack();
    }
}
