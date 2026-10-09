package compat.jeirouter.terminal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public record ItemReference(boolean curio, int slot) {
    public enum LocatorKind { STACK, INVENTORY_SLOT, UNSAFE }
    public LocatorKind locatorKind(Player player) {
        return get(player).isEmpty() ? LocatorKind.UNSAFE : curio ? LocatorKind.STACK : LocatorKind.INVENTORY_SLOT;
    }
    public ItemStack get(Player player) {
        if (slot < 0) return ItemStack.EMPTY;
        if (curio) return ModList.get().isLoaded("curios") ? CuriosAccess.get(player, slot) : ItemStack.EMPTY;
        return slot < player.getInventory().getContainerSize() ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }
    public void set(Player player, ItemStack stack) {
        if (curio) CuriosAccess.set(player, slot, stack);
        else player.getInventory().setItem(slot, stack);
    }
}
