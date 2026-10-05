package me.nedudis.nwv.client.browser;

import me.nedudis.nwv.client.render.WebBrowserTexture;
import me.nedudis.nwv.network.ScreenUrlUpdatePayload;
import me.nedudis.nwv.screen.ScreenData;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefDisplayHandlerAdapter;

public class BrowserInstance {
    private final   String      name;
    private         MCEFBrowser browser;
    private         ScreenData  data;
    private final   Identifier  textureId;
    private final   RenderType  renderType;
    private         boolean     isClosed = false;

    public static final String DEFAULT_URL = "https://www.google.com/";

    private double   lastDistance   = -1.0;
    private int      tickCounter    = 0;

    public BrowserInstance(ScreenData data) {
        this.name = data.name().toLowerCase();
        this.data = data;

        int pxWidth = (int) (data.widthBlocks() * 120);
        int pxHeight = (int) (data.heightBlocks() * 120);

        this.browser = MCEFApi.getInstance().createBrowser(data.url(), false);
        this.browser.resize(pxWidth, pxHeight);

        if (this.browser.getCefBrowser() != null && this.browser.getCefBrowser().getClient() != null) {
            this.browser.getCefBrowser().getClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                @Override
                public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
                    if (browser.getIdentifier() != BrowserInstance.this.browser.getCefBrowser().getIdentifier()) return false;
                    
                    if (target_url != null && !target_url.isEmpty()) {
                        browser.loadURL(target_url);
                    }
                    return true;
                }
            });

            this.browser.getCefBrowser().getClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
                @Override
                public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                    // Check if this event belongs to THIS specific browser instance via ID match!
                    if (browser.getIdentifier() != BrowserInstance.this.browser.getCefBrowser().getIdentifier()) return;

                    if (frame.isMain() && !url.equals(getData().url())) {
                        if (ClientPlayNetworking.canSend(ScreenUrlUpdatePayload.TYPE)) {
                            ClientPlayNetworking.send(new ScreenUrlUpdatePayload(getName(), url));
                        }
                    }
                }
            });
        }

        this.textureId = Identifier.fromNamespaceAndPath("nwv", "browser_" + name);
        WebBrowserTexture texture = new WebBrowserTexture(this.browser);
        Minecraft.getInstance().getTextureManager().register(this.textureId, texture);

        renderType = RenderTypes.entityTranslucent(this.textureId);
        System.out.println("[ NWV ] New screen has been created: " + name);
    }

    public Identifier getTextureId() { return textureId; }
    public String getName() { return name; }
    public ScreenData getData() { return data; }
    public MCEFBrowser getBrowser() { return browser; }
    public RenderType getRenderType() { return renderType; }
    public boolean isReady() {
        if (isClosed || browser == null) return false;

        return browser.getCefBrowser() != null && browser.getTextureView() != null;
    }

    public void updateData(ScreenData newData) {
        if (isClosed) return;
        boolean urlChanged = !this.data.url().equals(newData.url());
        boolean toggled = this.data.enabled() != newData.enabled();
        this.data = newData;

        if (this.browser.getCefBrowser() != null) {
            if (!newData.enabled()) {
                if (toggled) this.browser.getCefBrowser().loadURL("about:blank");
            } else {
                if (urlChanged || toggled) {
                    browser.getCefBrowser().loadURL(newData.url());
                }
            }
        }
    }

    public void updateVolume(LocalPlayer player) {
        if (!isReady() || data == null || !data.enabled()) return;

        tickCounter++;
        if (tickCounter % 5 != 0) return;

        Direction facing = data.facing() != null ? data.facing() : Direction.SOUTH;
        float rot = facing.toYRot();
        double rad = Math.toRadians(rot);

        double localCX = data.widthBlocks() / 2.0;
        double localCY = data.heightBlocks() / 2.0;

        double originX = data.pos().getX() + 0.5;
        double originY = data.pos().getY();
        double originZ = data.pos().getZ() + 0.5;

        double centerX = originX + (localCX * Math.cos(rad));
        double centerY = originY + localCY;
        double centerZ = originZ - (localCX * Math.sin(rad));

        double dist = Math.sqrt(player.distanceToSqr(centerX, centerY, centerZ));

        double roundedDist = Math.round(dist * 2.0) / 2.0;

        if (roundedDist != lastDistance) {
            lastDistance = roundedDist;
            CefBrowser cefBrowser = browser.getCefBrowser();
            if (cefBrowser != null) {
                double maxD = me.nedudis.nwv.client.NWVClientConfig.get().audioMaxDistance;
                double exp = me.nedudis.nwv.client.NWVClientConfig.get().audioDropoffExponent;
                
                String jsCode =
                        "if (!window.nwvVolumeHook) {" +
                                "  window.nwvVolumeHook = true;" +
                                "  const orig = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'volume');" +
                                "  Object.defineProperty(HTMLMediaElement.prototype, 'volume', {" +
                                "    get: function() { return this._nwvUserVol !== undefined ? this._nwvUserVol : orig.get.call(this); }," +
                                "    set: function(v) { " +
                                "      this._nwvUserVol = v; " +
                                "      let effectiveVol = 0.0;" +
                                "      if (window.nwvDist <= window.nwvMaxDist) {" +
                                "         let normalized = 1.0 - (window.nwvDist / window.nwvMaxDist);" +
                                "         effectiveVol = v * Math.pow(Math.max(0.0, normalized), window.nwvExp);" +
                                "      }" +
                                "      orig.set.call(this, effectiveVol);" +
                                "    }" +
                                "  });" +
                                "}" +
                                "window.nwvDist = " + roundedDist + ";" +
                                "window.nwvMaxDist = " + maxD + ";" +
                                "window.nwvExp = " + exp + ";" +
                                "document.querySelectorAll('video, audio').forEach(e => {" +
                                "  if (e._nwvUserVol === undefined) e._nwvUserVol = e.volume;" +
                                "  e.volume = e._nwvUserVol;" + // Trigger setter
                                "});";
                cefBrowser.executeJavaScript(jsCode, cefBrowser.getURL(), 0);
            }
        }
    }

    public void close() {
        if (isClosed) return;
        this.isClosed = true;
        if (this.browser != null) this.browser.close();
        Minecraft.getInstance().getTextureManager().release(this.textureId);
        System.out.println("[ NWV ] Screen has been deleted: " + name);
    }
}
