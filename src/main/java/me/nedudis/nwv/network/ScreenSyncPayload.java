package me.nedudis.nwv.network;

import me.nedudis.nwv.screen.ScreenData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

public record ScreenSyncPayload(List<ScreenData> screens) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("nwv", "screen_sync");
    public static final Type<ScreenSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.fromCodec(ScreenData.CODEC)),
            ScreenSyncPayload::screens,
            ScreenSyncPayload::new
    );

    @Override
    @NullMarked
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
