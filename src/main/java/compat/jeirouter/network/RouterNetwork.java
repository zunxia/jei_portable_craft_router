package compat.jeirouter.network;

import java.util.function.Consumer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class RouterNetwork {
    // 服务端类不直接引用 Minecraft 客户端类型，保证专用服务端能独立加载。
    public static Consumer<OpenTerminalResultPayload> clientReceiver = r -> {};
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToClient(SnapshotSupportPayload.TYPE, SnapshotSupportPayload.CODEC, (p, c) -> {});
        registrar.playToClient(PatternSupportPayload.TYPE, PatternSupportPayload.CODEC, (p, c) -> {});
        registrar.playToServer(OpenTerminalRequestPayload.TYPE, OpenTerminalRequestPayload.CODEC,
            (p, c) -> c.enqueueWork(() -> OpenTerminalHandler.handle(p, c)));
        registrar.playToClient(OpenTerminalResultPayload.TYPE, OpenTerminalResultPayload.CODEC,
            (p, c) -> c.enqueueWork(() -> clientReceiver.accept(p)));
    }
    private RouterNetwork() {}
}
