package compat.jeirouter.apply;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageItem;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;

public final class IntegratedRecipeApplier {
    public static boolean ensureCraftingTab(AbstractContainerMenu menu) {
        if (!(menu instanceof ContainerTerminalStorageItem terminal)) return true;
        var tabId = TerminalStorageTabIngredientComponentItemStackCrafting.NAME;
        if (tabId == null) return false;
        String tab = tabId.toString();
        if (tab.equals(terminal.getSelectedTab())) return true;
        if (!terminal.getTabsCommon().containsKey(tab)) return false;
        terminal.setSelectedTab(tab);
        terminal.sendGuiStateToServer();
        return tab.equals(terminal.getSelectedTab());
    }
    private IntegratedRecipeApplier() {}
}
