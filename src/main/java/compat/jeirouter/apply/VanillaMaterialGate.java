package compat.jeirouter.apply;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import mezz.jei.common.Internal;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class VanillaMaterialGate {
    public static MaterialGate.GateResult check(AbstractContainerMenu menu, RecipeSnapshot snapshot) {
        if (!(menu instanceof CraftingMenu) || menu.slots.size() < 10) return MaterialGate.unknown();
        return checkSlots(menu, snapshot, menu.slots.subList(1, 10));
    }
    public static MaterialGate.GateResult preview(RecipeSnapshot snapshot) {
        var player = Minecraft.getInstance().player;
        if (player == null) return MaterialGate.unknown();
        var slots = new ArrayList<Slot>();
        var container = new SimpleContainer(9);
        for (int i = 0; i < 9; i++) slots.add(new Slot(container, i, 0, 0));
        return checkSlots(player.inventoryMenu, snapshot, slots);
    }
    private static MaterialGate.GateResult checkSlots(AbstractContainerMenu menu, RecipeSnapshot snapshot, List<Slot> craftingSlots) {
        var player = Minecraft.getInstance().player;
        if (player == null || snapshot.craftingRecipe() == null) return MaterialGate.unknown();
        var available = new LinkedHashMap<Slot, ItemStack>();
        for (var slot : menu.slots) {
            if ((slot.container == player.getInventory() || craftingSlots.contains(slot)) && slot.mayPickup(player) && slot.hasItem()) {
                available.put(slot, slot.getItem().copy());
            }
        }
        var result = RecipeTransferUtil.getRecipeTransferOperations(Internal.getJeiRuntime().getJeiHelpers().getStackHelper(), available, snapshot.inputs(), craftingSlots);
        return result.missingItems.isEmpty() ? MaterialGate.enough() : MaterialGate.missing(result.missingItems.size());
    }
    private VanillaMaterialGate() {}
}
