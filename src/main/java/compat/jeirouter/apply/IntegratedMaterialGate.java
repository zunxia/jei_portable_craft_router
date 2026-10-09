package compat.jeirouter.apply;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageItem;

public final class IntegratedMaterialGate {
    public static boolean supports(AbstractContainerMenu menu) { return menu instanceof ContainerTerminalStorageItem; }
    public static MaterialGate.GateResult check(AbstractContainerMenu menu, RecipeSnapshot snapshot) {
        if (!IntegratedRecipeApplier.ensureCraftingTab(menu)) return MaterialGate.unknown();
        var player = Minecraft.getInstance().player;
        if (player == null) return MaterialGate.unknown();
        var terminal = (ContainerTerminalStorageItem) menu;
        if (!(terminal.getTabCommon(terminal.getSelectedTab()) instanceof org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCraftingCommon crafting) ||
            !(terminal.getTabClient(terminal.getSelectedTab()) instanceof org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentClient<?, ?>)) return MaterialGate.unknown();
        // 现成 handler 的 dry-run 会缓存上一菜单的结果。复用其公开的无缓存检查，
        // 使用相同的匹配条件和同步网络库存，不调用私有方法、不发送搬运包。
        var inputs = snapshot.inputs().stream().map(org.cyclops.integratedterminalscompat.modcompat.jei.RecipeInputSlotJei::new).toList();
        var result = org.cyclops.integratedterminalscompat.modcompat.common.RecipeTransferHelpers.getMissingItemsUncached(
            terminal, inputs, player, crafting, org.cyclops.integratedterminalscompat.modcompat.jei.JEIIntegratedTerminalsConfig::getItemStackMatchCondition, id -> {});
        if (result.isEmpty() || result.get().getSlotsMissing().isEmpty()) return MaterialGate.enough();
        return MaterialGate.missing(result.get().getSlotsMissing().size());
    }
    private IntegratedMaterialGate() {}
}
