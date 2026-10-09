package compat.jeirouter.apply;

import compat.jeirouter.JeiPortableRouter;
import mezz.jei.common.Internal;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.neoforged.fml.ModList;

public final class MaterialGate {
    public enum Verdict { ENOUGH, CRAFTABLE, NOT_ENOUGH, UNKNOWN }
    public record GateResult(Verdict verdict, int missingCount, Component reason) {
        public boolean enough() { return verdict == Verdict.ENOUGH; }
    }
    public static GateResult enough() { return new GateResult(Verdict.ENOUGH, 0, Component.empty()); }
    public static GateResult craftable(int count) { return new GateResult(Verdict.CRAFTABLE, count, Component.translatable("router.ctrl_craft").withStyle(net.minecraft.ChatFormatting.BLUE)); }
    public static GateResult missing(int count) { return new GateResult(Verdict.NOT_ENOUGH, Math.max(1, count), Component.translatable("router.apply_failed")); }
    public static GateResult unknown() { return new GateResult(Verdict.UNKNOWN, 0, Component.translatable("router.apply_failed")); }
    public static GateResult unsupported() { return new GateResult(Verdict.UNKNOWN, 0, Component.translatable("router.jei_cannot_apply")); }
    public static GateResult check(AbstractContainerMenu menu, RecipeSnapshot snapshot) {
        try {
            var runtime = Internal.getOptionalJeiRuntime();
            if (runtime.isEmpty() || snapshot.craftingRecipe() == null) return unknown();
            if (runtime.get().getRecipeTransferManager().getRecipeTransferHandler(menu, snapshot.layout().getRecipeCategory()).isEmpty()) return unsupported();
            var mods = ModList.get();
            if (mods.isLoaded("ae2") && Ae2MaterialGate.supports(menu)) return Ae2MaterialGate.check(menu, snapshot);
            if (mods.isLoaded("integratedterminals") && IntegratedMaterialGate.supports(menu)) return IntegratedMaterialGate.check(menu, snapshot);
            if (menu instanceof CraftingMenu) return VanillaMaterialGate.check(menu, snapshot);
            return unknown();
        } catch (RuntimeException | LinkageError e) {
            JeiPortableRouter.LOG.warn("Material precheck unavailable for {}", snapshot.recipeId(), e);
            return unknown();
        }
    }
    // 未打开终端时没有绑定网络的同步库存；只对无网络的工作台预览，绝不把背包缺料当作网络缺料。
    public static GateResult preview(RecipeSnapshot snapshot, String adapterId) {
        if (!"stick".equals(adapterId)) return unknown();
        try { return VanillaMaterialGate.preview(snapshot); }
        catch (RuntimeException | LinkageError e) { return unknown(); }
    }
    private MaterialGate() {}
}
