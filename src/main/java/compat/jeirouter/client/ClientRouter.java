package compat.jeirouter.client;

import compat.jeirouter.JeiPortableRouter;
import compat.jeirouter.RouterConfig;
import compat.jeirouter.apply.RecipeApplier;
import compat.jeirouter.apply.RecipeSnapshot;
import compat.jeirouter.network.OpenTerminalRequestPayload;
import compat.jeirouter.network.OpenTerminalResultPayload;
import compat.jeirouter.terminal.TerminalPriorityResolver;
import java.util.List;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.common.Internal;
import compat.jeirouter.apply.JeiTransferService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientRouter {
    private static final String TOO_LARGE = "jei.tooltip.error.recipe.transfer.too.large.player.inventory";
    private static Pending pending;
    private static int sequence, ticks;
    private static boolean broken;
    private static ClientLevel notifiedUnsupportedLevel;
    private static final class Pending {
        final RecipeSnapshot snapshot;
        final JeiTransferService service;
        final List<TerminalPriorityResolver.Candidate> candidates;
        final Screen originalScreen;
        final ClientLevel level;
        final boolean pattern, craftMissing;
        final net.minecraft.world.inventory.AbstractContainerMenu originalMenu;
        int candidateIndex, requestId, deadline, readyAt;
        OpenTerminalResultPayload result;
        Pending(RecipeSnapshot snapshot, JeiTransferService service, List<TerminalPriorityResolver.Candidate> candidates) {
            this(snapshot, service, candidates, false, Screen.hasControlDown());
        }
        Pending(RecipeSnapshot snapshot, JeiTransferService service, List<TerminalPriorityResolver.Candidate> candidates, boolean pattern, boolean craftMissing) {
            this.snapshot = snapshot; this.service = service; this.candidates = candidates;
            originalScreen = Minecraft.getInstance().screen; level = Minecraft.getInstance().level;
            this.pattern = pattern; this.craftMissing = craftMissing;
            originalMenu = Minecraft.getInstance().player.containerMenu;
        }
    }
    public static boolean isOversizedInventoryRecipe(IRecipeLayoutDrawable<?> layout,
            AbstractContainerScreen<?> source, IRecipeTransferError error) {
        var mc = Minecraft.getInstance();
        if (broken || !RouterConfig.ENABLED.get() || mc.player == null || source == null ||
            !(source.getMenu() instanceof InventoryMenu) || mc.player.containerMenu != mc.player.inventoryMenu ||
            !RecipeTypes.CRAFTING.equals(layout.getRecipeCategory().getRecipeType()) ||
            !(layout.getRecipe() instanceof RecipeHolder<?> holder) || !(holder.value() instanceof CraftingRecipe) ||
            error == null || error.getType() != IRecipeTransferError.Type.USER_FACING) return false;
        // 严格检查原始错误翻译键，避免误接管缺材料、无服务端支持等其他拒绝路径。
        var tooltip = new ErrorTooltipCollector();
        error.getTooltip(tooltip);
        return tooltip.toLegacyToComponents().stream().anyMatch(c -> containsKey(c, TOO_LARGE));
    }
    private static boolean containsKey(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents t && key.equals(t.getKey())) return true;
        return component.getSiblings().stream().anyMatch(c -> containsKey(c, key));
    }
    public static List<TerminalPriorityResolver.Candidate> candidates() {
        return TerminalPriorityResolver.find(Minecraft.getInstance().player);
    }
    public static boolean serverSupported() {
        var mc = Minecraft.getInstance();
        return mc.getConnection() != null && mc.getConnection().hasChannel(OpenTerminalRequestPayload.TYPE) &&
            mc.getConnection().hasChannel(compat.jeirouter.network.SnapshotSupportPayload.TYPE) &&
            JeiTransferService.serverSupported();
    }
    public static boolean canRoute(IRecipeLayoutDrawable<?> layout, AbstractContainerScreen<?> source, IRecipeTransferError error) {
        return pending == null && isOversizedInventoryRecipe(layout, source, error) && serverSupported() && !candidates().isEmpty();
    }
    public static void begin(IRecipeLayoutDrawable<?> layout, JeiTransferService service, boolean maxTransfer) {
        begin(layout, service, maxTransfer, Screen.hasControlDown());
    }
    public static void begin(IRecipeLayoutDrawable<?> layout, JeiTransferService service, boolean maxTransfer, boolean craftMissing) {
        if (pending != null || !serverSupported()) return;
        var candidates = candidates();
        if (candidates.isEmpty()) return;
        pending = new Pending(RecipeSnapshot.capture(layout, maxTransfer), service, candidates, false, craftMissing);
        if (RouterConfig.DEBUG.get()) JeiPortableRouter.LOG.info("Route {}: {}", pending.snapshot.recipeId(), candidates);
        sendNext();
    }
    public static boolean patternSupported() {
        var mc = Minecraft.getInstance();
        return !broken && serverSupported() && mc.getConnection().hasChannel(compat.jeirouter.network.PatternSupportPayload.TYPE);
    }
    public static boolean idle() { return !broken && pending == null; }
    public static void beginPattern(IRecipeLayoutDrawable<?> layout, JeiTransferService service) {
        if (!idle() || !patternSupported()) return;
        var candidates = TerminalPriorityResolver.findPatterns(Minecraft.getInstance().player);
        if (candidates.isEmpty()) return;
        pending = new Pending(RecipeSnapshot.capture(layout, false), service, candidates, true, false);
        sendNext();
    }
    private static void sendNext() {
        var mc = Minecraft.getInstance();
        var p = pending;
        if (p == null || mc.player == null || mc.player.containerMenu != p.originalMenu || mc.screen == null) {
            pending = null; return;
        }
        if (p.candidateIndex >= p.candidates.size()) {
            message(Component.translatable(p.pattern ? "router.pattern_open_failed" : TOO_LARGE)); pending = null; return;
        }
        var candidate = p.candidates.get(p.candidateIndex);
        p.requestId = ++sequence;
        p.deadline = ticks + 200;
        p.result = null;
        var ref = candidate.reference();
        PacketDistributor.sendToServer(new OpenTerminalRequestPayload(p.requestId, ref.curio(), ref.slot(), candidate.adapter().id(), mc.player.containerMenu.containerId));
    }
    public static void receive(OpenTerminalResultPayload result) {
        var p = pending;
        if (p == null || result.requestId() != p.requestId || Minecraft.getInstance().level != p.level) return;
        if (!result.success()) {
            var candidate = p.candidates.get(p.candidateIndex);
            message(Component.translatable("router.candidate_failed", candidate.name(), result.reason()));
            p.candidateIndex++;
            sendNext();
        } else {
            p.result = result;
            p.readyAt = ticks + 5;
            p.deadline = ticks + 60;
        }
    }
    public static void tick() {
        ticks++;
        var game = Minecraft.getInstance();
        if (game.level == null) notifiedUnsupportedLevel = null;
        if (!broken && RouterConfig.ENABLED.get() && game.level != null && game.level != notifiedUnsupportedLevel &&
            game.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen &&
            Internal.getOptionalJeiRuntime().isPresent() && !serverSupported() && !candidates().isEmpty()) {
            message(Component.translatable("router.server_jei"));
            notifiedUnsupportedLevel = game.level;
        }
        var p = pending;
        if (p == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level != p.level || !RouterConfig.ENABLED.get() || mc.screen == null) { pending = null; return; }
        if (ticks > p.deadline) {
            if (p.result != null && mc.player.containerMenu == p.originalMenu) {
                message(Component.translatable("router.timeout"));
                if (mc.getConnection() != null) mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(p.result.containerId()));
                p.candidateIndex++; p.result = null; sendNext();
            } else { message(Component.translatable("router.timeout")); pending = null; }
            return;
        }
        if (p.result == null) {
            if (mc.player.containerMenu == p.originalMenu && mc.screen != p.originalScreen) pending = null;
            return;
        }
        if (ticks < p.readyAt) return;
        var menu = mc.player.containerMenu;
        if (menu.containerId != p.result.containerId()) {
            // 尚未收到菜单打开包时继续等；玩家切到另一个菜单则取消。
            if (menu != p.originalMenu) pending = null;
            return;
        }
        if (!BuiltInRegistries.MENU.getKey(menu.getType()).toString().equals(p.result.menuId()) ||
            !(mc.screen instanceof AbstractContainerScreen<?> screen) || screen.getMenu() != menu) { pending = null; return; }
        pending = null; // 先消耗请求，确保后续 tick 不重复转移。
        try {
            if (p.pattern) {
                if (!PatternRecipeSupport.apply(p.snapshot.layout(), p.service, screen)) message(Component.translatable("router.jei_cannot_apply"));
            } else {
                var applied = RecipeApplier.apply(p.snapshot, p.service, screen, p.craftMissing);
                if (!applied.success()) {
                    message(applied.reason());
                    if (containsKey(applied.reason(), "router.ctrl_craft")) showRecipe(p.snapshot.layout());
                }
            }
        } catch (RuntimeException | LinkageError e) {
            JeiPortableRouter.LOG.warn("Recipe apply failed for {}", p.snapshot.recipeId(), e);
            message(Component.translatable("router.apply_failed"));
        }
    }
    private static <T> void showRecipe(IRecipeLayoutDrawable<T> layout) {
        Internal.getOptionalJeiRuntime().ifPresent(runtime -> runtime.getRecipesGui()
            .showRecipes(layout.getRecipeCategory(), List.of(layout.getRecipe()), List.of()));
    }
    public static void failSoft(Throwable error) {
        if (!broken) JeiPortableRouter.LOG.warn("JEI routing disabled after incompatible API", error);
        broken = true; pending = null;
    }
    private static void message(Component text) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(text, false);
    }
    private ClientRouter() {}
}
