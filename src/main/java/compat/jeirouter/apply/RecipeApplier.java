package compat.jeirouter.apply;

import compat.jeirouter.JeiPortableRouter;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class RecipeApplier {
    public record ApplyResult(boolean success, net.minecraft.network.chat.Component reason) {}
    public static ApplyResult apply(RecipeSnapshot snapshot, JeiTransferService service, AbstractContainerScreen<?> screen) {
        return apply(snapshot, service, screen, net.minecraft.client.gui.screens.Screen.hasControlDown());
    }
    public static ApplyResult apply(RecipeSnapshot snapshot, JeiTransferService service, AbstractContainerScreen<?> screen, boolean craftMissing) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !JeiTransferService.serverSupported()) return new ApplyResult(false, net.minecraft.network.chat.Component.translatable("router.server_jei"));
        var gate = MaterialGate.check(screen.getMenu(), snapshot);
        if (gate.verdict() == MaterialGate.Verdict.CRAFTABLE && craftMissing) {
            var handler = Internal.getJeiRuntime().getRecipeTransferManager().getRecipeTransferHandler(screen.getMenu(), snapshot.layout().getRecipeCategory());
            if (handler.isEmpty() || !handler.get().getClass().getName().equals("tamaized.ae2jeiintegration.integration.modules.jei.transfer.UseCraftingRecipeTransfer"))
                return new ApplyResult(false, MaterialGate.unsupported().reason());
            var error = service.getTransferRecipeError(screen, snapshot.layout(), mc.player);
            if (error.isPresent() && !error.get().getType().allowsTransfer) return new ApplyResult(false, MaterialGate.unsupported().reason());
            return new ApplyResult(Ae2MaterialGate.craftMissing(screen.getMenu(), snapshot), gate.reason());
        }
        if (!gate.enough()) {
            JeiPortableRouter.LOG.info("Material gate {}: {} (slots {})", snapshot.recipeId(), gate.verdict(), gate.missingCount());
            return new ApplyResult(false, gate.reason());
        }
        // 通过 JEI 的服务调用新菜单的 handler：AE2/IT 使用各自网络，原版工作台由 JEI
        // 决定 counted/non-counted 封包，处理完整套数及所有槽位安全检查。
        boolean result = service.transferRecipe(screen, snapshot.layout(), mc.player, snapshot.maxTransfer());
        if (!result) JeiPortableRouter.LOG.info("Opened terminal; JEI could not apply {}", snapshot.recipeId());
        return new ApplyResult(result, net.minecraft.network.chat.Component.translatable("router.jei_cannot_apply"));
    }
    private RecipeApplier() {}
}
