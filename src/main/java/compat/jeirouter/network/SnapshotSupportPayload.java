package compat.jeirouter.network;

import compat.jeirouter.JeiPortableRouter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 仅用握手的通道存在性确认双方支持新定位器，不需要发送实际探测包。 */
public record SnapshotSupportPayload() implements CustomPacketPayload {
    public static final Type<SnapshotSupportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JeiPortableRouter.ID, "snapshot_support"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotSupportPayload> CODEC = StreamCodec.unit(new SnapshotSupportPayload());
    public Type<SnapshotSupportPayload> type() { return TYPE; }
}
