package me.nedudis.nwv.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record ScreenData(String name, BlockPos pos, Direction facing, String url, boolean enabled,
                         float widthBlocks, float heightBlocks) {
    public static final Codec<ScreenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ScreenData::name),
            BlockPos.CODEC.fieldOf("pos").forGetter(ScreenData::pos),
            Direction.CODEC.fieldOf("facing").forGetter(ScreenData::facing),
            Codec.STRING.fieldOf("url").forGetter(ScreenData::url),
            Codec.BOOL.fieldOf("enabled").forGetter(ScreenData::enabled),
            Codec.FLOAT.optionalFieldOf("width_blocks", 1.0f).forGetter(ScreenData::widthBlocks),
            Codec.FLOAT.optionalFieldOf("height_blocks", 1.0f).forGetter(ScreenData::heightBlocks)
        ).apply(instance, ScreenData::new));

    public ScreenData withEnabled(boolean newEnabled) {
        return new ScreenData(name, pos, facing, url, newEnabled, widthBlocks, heightBlocks);
    }

    public ScreenData withUrl(String newUrl) {
        return new ScreenData(name, pos, facing, newUrl, enabled, widthBlocks, heightBlocks);
    }
}
