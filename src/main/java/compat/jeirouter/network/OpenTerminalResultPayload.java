package compat.jeirouter.network;

import compat.jeirouter.JeiPortableRouter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTerminalResultPayload(int requestId, boolean success, int containerId, String menuId, Component reason) implements CustomPacketPayload {
    public static final Type<OpenTerminalResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JeiPortableRouter.ID, "result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTerminalResultPayload> CODEC = StreamCodec.ofMember(
        OpenTerminalResultPayload::write, b -> new OpenTerminalResultPayload(b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readUtf(128), ComponentSerialization.STREAM_CODEC.decode(b)));
    private void write(RegistryFriendlyByteBuf b) {
        b.writeVarInt(requestId); b.writeBoolean(success); b.writeVarInt(containerId); b.writeUtf(menuId, 128);
        ComponentSerialization.STREAM_CODEC.encode(b, reason);
    }
    public Type<OpenTerminalResultPayload> type() { return TYPE; }
}
