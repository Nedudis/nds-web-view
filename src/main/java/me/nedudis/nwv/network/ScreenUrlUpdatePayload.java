package me.nedudis.nwv.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

public record ScreenUrlUpdatePayload(String screenName, String newUrl) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("nwv", "screen_url_update");
    public static final Type<ScreenUrlUpdatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenUrlUpdatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.screenName());
                buf.writeUtf(payload.newUrl());
            },
            buf -> new ScreenUrlUpdatePayload(
                    buf.readUtf(),
                    buf.readUtf()
            )
    );

    @Override
    @NullMarked
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
