package me.nedudis.nwv.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

public record ScreenInteractPayload(String screenName, int actionType, double x, double y, int buttonOrKey, int modifiers) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("nwv", "screen_interact");
    public static final Type<ScreenInteractPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenInteractPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.screenName());
                buf.writeInt(payload.actionType());
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.y());
                buf.writeInt(payload.buttonOrKey());
                buf.writeInt(payload.modifiers());
            },
            buf -> new ScreenInteractPayload(
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    @NullMarked
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Actions {
        public static final int MOUSE_DOWN = 0;
        public static final int MOUSE_UP   = 1;
        public static final int SCROLL     = 2;
        public static final int KEY_DOWN   = 3;
        public static final int KEY_UP     = 4;
        public static final int CHAR_TYPED = 5;
    }
}
