package compat.jeirouter.network;

import compat.jeirouter.JeiPortableRouter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTerminalRequestPayload(int requestId, boolean curio, int slot, String adapterId, int sourceMenuId) implements CustomPacketPayload {
    public static final Type<OpenTerminalRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JeiPortableRouter.ID, "open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTerminalRequestPayload> CODEC = StreamCodec.ofMember(
        OpenTerminalRequestPayload::write, b -> new OpenTerminalRequestPayload(b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readUtf(32), b.readVarInt()));
    private void write(RegistryFriendlyByteBuf b) {
        b.writeVarInt(requestId); b.writeBoolean(curio); b.writeVarInt(slot); b.writeUtf(adapterId, 32); b.writeVarInt(sourceMenuId);
    }
    public Type<OpenTerminalRequestPayload> type() { return TYPE; }
}
