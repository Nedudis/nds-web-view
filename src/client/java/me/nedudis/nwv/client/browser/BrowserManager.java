package me.nedudis.nwv.client.browser;


import me.nedudis.nwv.client.render.WebBrowserTexture;
import me.nedudis.nwv.screen.ScreenData;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.cef.browser.CefBrowser;
import org.jspecify.annotations.Nullable;

public class BrowserManager {

    public static final double  DEFAULT_ORIGIN_X = 0.5;
    public static final double  DEFAULT_ORIGIN_Y = 70;
    public static final double  DEFAULT_ORIGIN_Z = 0.5;

    public static final int     BROWSER_WIDTH   = 1920;
    public static final int     BROWSER_HEIGHT  = 1080;

    public static final int     BROWSER_WIDTH_BLOCKS    = 16;
    public static final int     BROWSER_HEIGHT_BLOCKS   = 9;

    private static MCEFBrowser  browser;
    private static Identifier   textureId;
    private static RenderType   renderType;
    private static boolean      initialized = false;
    private static @Nullable String url     = null;

    private static BlockPos     screenPos       = null;
    private static boolean      screenEnabled   = false;
    private static Direction    screenFacing    = null;

    private static double   lastDistance = -1.0;
    private static int      tickCounter = 0;

    private static final String DEFAULT_URL = "https://www.google.com/";

    public static void init() {
        if (initialized || MCEFApi.getInstance() == null) return;

        browser = MCEFApi.getInstance().createBrowser(DEFAULT_URL, false);
        browser.resize(BROWSER_WIDTH, BROWSER_HEIGHT);

        textureId = Identifier.fromNamespaceAndPath("nwv", "test_browser");
        WebBrowserTexture texture = new WebBrowserTexture(browser);
        Minecraft.getInstance().getTextureManager().register(textureId, texture);

        renderType = RenderTypes.text(textureId);

        initialized = true;
        System.out.println("[ nwv ] - INITIALIZED");

    }

    public static void updateVolume(LocalPlayer player) {
        if (!isReady() || !isActive() || screenPos == null) return;

        tickCounter++;
        if (tickCounter % 5 != 0) return;

        Direction facing = getFacing();
        float rot = facing.toYRot();
        double rad = Math.toRadians(rot);

        double localCX = BROWSER_WIDTH_BLOCKS / 2.0;
        double localCY = BROWSER_HEIGHT_BLOCKS / 2.0;

        double centerX = screenPos.getX() + (localCX * Math.cos(rad));
        double centerY = screenPos.getY() + localCY;
        double centerZ = screenPos.getZ() + (localCX * Math.sin(rad));

        double dist = Math.sqrt(player.distanceToSqr(centerX, centerY, centerZ));

        double roundedDist = Math.round(dist * 2.0) / 2.0;

        if (roundedDist != lastDistance) {
            lastDistance = roundedDist;
            CefBrowser cefBrowser = browser.getCefBrowser();
            if (cefBrowser != null) {
                String jsCode =
                        "if (!window.nwvVolumeHook) {" +
                        "  window.nwvVolumeHook = true;" +
                        "  const orig = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'volume');" +
                        "  Object.defineProperty(HTMLMediaElement.prototype, 'volume', {" +
                        "    get: function() { return this._nwvUserVol !== undefined ? this._nwvUserVol : orig.get.call(this); }," +
                        "    set: function(v) { " +
                        "      this._nwvUserVol = v; " +
                        "      let maxHearableBlocks = 50.0; " +
                        "      let effectiveVol = Math.max(0.0, v - (window.nwvDist / maxHearableBlocks));" +
                        "      orig.set.call(this, effectiveVol);" +
                        "    }" +
                        "  });" +
                        "}" +
                        "window.nwvDist = " + roundedDist + ";" +
                        "document.querySelectorAll('video, audio').forEach(e => {" +
                        "  if (e._nwvUserVol === undefined) e._nwvUserVol = e.volume;" +
                        "  e.volume = e._nwvUserVol;" +
                        "});";
                cefBrowser.executeJavaScript(jsCode, cefBrowser.getURL(), 0);
            }
        }
    }

    public static void applySync(ScreenData data) {
        if (browser == null) init();

        screenPos = data.pos();
        screenEnabled = data.enabled();
        screenFacing = data.facing();
        setUrl(data.url());
    }

    public static double getOriginX() { return screenPos != null ? screenPos.getX() + 0.5 : DEFAULT_ORIGIN_X; }
    public static double getOriginY() { return screenPos != null ? screenPos.getY() : DEFAULT_ORIGIN_Y; }
    public static double getOriginZ() { return screenPos != null ? screenPos.getZ() - 0.5 : DEFAULT_ORIGIN_Z; }

    public static MCEFBrowser getBrowser() {
        return browser;
    }

    public static RenderType getRenderType() {
        return renderType;
    }

    public static Direction getFacing() {
        return screenFacing != null ? screenFacing : Direction.SOUTH;
    }

    public static boolean isActive() {
        return screenEnabled && screenPos != null;
    }

    public static boolean isReady() {
        return initialized && browser != null && browser.getTextureView() != null;
    }

    public static void setUrl(String url) {
        if (browser == null) init();
        CefBrowser cefBrowser = browser.getCefBrowser();
        if (cefBrowser != null) cefBrowser.loadURL(url);
        BrowserManager.url = url;
    }

    public static void loadDefaultUrl() {
        CefBrowser cefBrowser = browser.getCefBrowser();
        if (cefBrowser == null) return;

        cefBrowser.loadURL(DEFAULT_URL);
    }

    public static void goForward() {
        if (browser == null) return;

        CefBrowser cefBrowser = browser.getCefBrowser();
        if (cefBrowser == null) return;

        if (cefBrowser.canGoForward())
            cefBrowser.goForward();

    }

    public static void goBack() {
        if (browser == null) return;

        CefBrowser cefBrowser = browser.getCefBrowser();
        if (cefBrowser == null) return;

        if (cefBrowser.canGoBack())
            cefBrowser.goBack();
    }
}
