package me.nedudis.nwv.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WebBrowserTexture extends AbstractTexture {
    private final MCEFBrowser browser;

    public WebBrowserTexture(MCEFBrowser browser) {
        this.browser = browser;
    }

    @Override
    public GpuTextureView getTextureView() {
        GpuTextureView view = this.browser.getTextureView();
        if (view != null) return view;

        return Minecraft.getInstance()
                .getTextureManager()
                .getTexture(MissingTextureAtlasSprite.getLocation())
                .getTextureView();
    }
}
