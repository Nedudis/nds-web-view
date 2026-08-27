package me.nedudis.nwv.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class WebBrowserTexture extends AbstractTexture {
    private final MCEFBrowser browser;

    public WebBrowserTexture(MCEFBrowser browser) {
        this.browser = browser;
    }

    @Override
    public GpuTextureView getTextureView() {
        return this.browser.getTextureView();
    }
}
