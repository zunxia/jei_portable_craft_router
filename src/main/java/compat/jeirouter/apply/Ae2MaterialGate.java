package compat.jeirouter.apply;

import appeng.menu.me.items.CraftingTermMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class Ae2MaterialGate {
    public static boolean supports(AbstractContainerMenu menu) { return menu instanceof CraftingTermMenu; }
    public static MaterialGate.GateResult check(AbstractContainerMenu menu, RecipeSnapshot snapshot) {
        var recipe = snapshot.craftingRecipe();
        var helper = RouterJeiPlugin.transferHelper();
        if (!(menu instanceof CraftingTermMenu terminal) || recipe == null || helper == null) return MaterialGate.unknown();
        var missing = terminal.findMissingIngredients(helper.getGuiSlotIndexToIngredientMap(recipe));
        // 可合成与现货分开；只有用户明确 Ctrl 点击才启动网络合成。
        return missing.anyMissing() ? MaterialGate.missing(missing.missingSlots().size()) :
            missing.anyCraftable() ? MaterialGate.craftable(missing.craftableSlots().size()) : MaterialGate.enough();
    }
    public static CraftingTermMenu.MissingIngredientSlots ingredients(AbstractContainerMenu menu, RecipeSnapshot snapshot) {
        var helper = RouterJeiPlugin.transferHelper();
        return menu instanceof CraftingTermMenu terminal && snapshot.craftingRecipe() != null && helper != null
            ? terminal.findMissingIngredients(helper.getGuiSlotIndexToIngredientMap(snapshot.craftingRecipe())) : null;
    }
    public static boolean craftMissing(AbstractContainerMenu menu, RecipeSnapshot snapshot) {
        if (!(menu instanceof CraftingTermMenu terminal) || snapshot.craftingRecipe() == null) return false;
        // 与原 AE2 JEI handler 完全相同的公开入口，显式传入点击时保存的 Ctrl 意图。
        appeng.integration.modules.itemlists.CraftingHelper.performTransfer(terminal, snapshot.recipeId(), snapshot.craftingRecipe().value(), true);
        return true;
    }
    private Ae2MaterialGate() {}
}
