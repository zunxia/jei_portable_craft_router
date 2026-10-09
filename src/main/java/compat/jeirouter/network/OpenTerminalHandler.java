package compat.jeirouter.network;

import compat.jeirouter.JeiPortableRouter;
import compat.jeirouter.RouterConfig;
import compat.jeirouter.terminal.ItemReference;
import compat.jeirouter.terminal.TerminalPriorityResolver;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class OpenTerminalHandler {
    private static final WeakHashMap<ServerPlayer, long[]> budgets = new WeakHashMap<>();
    public static void handle(OpenTerminalRequestPayload p, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        Component reason;
        try {
            reason = validateAndOpen(p, player);
        } catch (RuntimeException | LinkageError e) {
            JeiPortableRouter.LOG.warn("Terminal open failed for {}", p.adapterId(), e);
            reason = Component.translatable("router.open_failed");
        }
        boolean success = reason == null && player.containerMenu != player.inventoryMenu;
        if (success) player.containerMenu.broadcastChanges();
        String menuId = success ? BuiltInRegistries.MENU.getKey(player.containerMenu.getType()).toString() : "";
        context.reply(new OpenTerminalResultPayload(p.requestId(), success, success ? player.containerMenu.containerId : -1,
            menuId, reason != null ? reason : Component.empty()));
    }
    private static Component validateAndOpen(OpenTerminalRequestPayload p, ServerPlayer player) {
        if (!RouterConfig.ENABLED.get()) return Component.translatable("router.disabled");
        if (!ModList.get().isLoaded("jei")) return Component.translatable("router.server_jei");
        boolean pattern = p.adapterId().equals("ae2_pattern") || p.adapterId().equals("ae2_universal_pattern");
        if (!player.isAlive() || player.isSpectator() || (!pattern && player.containerMenu != player.inventoryMenu) ||
            p.sourceMenuId() != player.containerMenu.containerId) return Component.translatable("router.stale_request");
        var budget = budgets.computeIfAbsent(player, x -> new long[2]);
        long tick = player.serverLevel().getGameTime();
        if (tick - budget[0] >= 20 || tick < budget[0]) { budget[0] = tick; budget[1] = 0; }
        if (++budget[1] > 20) return Component.translatable("router.rate_limit");
        var adapter = java.util.stream.Stream.concat(TerminalPriorityResolver.adapters().stream(), TerminalPriorityResolver.patternAdapters().stream())
            .filter(a -> a.id().equals(p.adapterId())).findFirst().orElse(null);
        // 1.0 客户端不认识快照定位器；提前拒绝 AE2 打开，允许其降级到其他终端。
        if (p.adapterId().startsWith("ae2_") && !player.connection.hasChannel(SnapshotSupportPayload.TYPE)) return Component.translatable("router.open_failed");
        var ref = new ItemReference(p.curio(), p.slot());
        var stack = ref.get(player);
        if (adapter == null || stack.isEmpty() || ref.locatorKind(player) == ItemReference.LocatorKind.UNSAFE || !adapter.matches(stack)) return Component.translatable("router.invalid_item");
        if (RouterConfig.DEBUG.get()) JeiPortableRouter.LOG.info("Open {} at {}", adapter.id(), ref);
        return adapter.open(player, ref, stack);
    }
    private OpenTerminalHandler() {}
}
