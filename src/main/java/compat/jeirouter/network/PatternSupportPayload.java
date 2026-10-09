package compat.jeirouter.network;

import compat.jeirouter.JeiPortableRouter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 可选握手标记，避免向旧服务端请求未注册的样板适配器。
public record PatternSupportPayload() implements CustomPacketPayload {
    public static final Type<PatternSupportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JeiPortableRouter.ID, "pattern_support"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PatternSupportPayload> CODEC = StreamCodec.unit(new PatternSupportPayload());
    public Type<PatternSupportPayload> type() { return TYPE; }
}
