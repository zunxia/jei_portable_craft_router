package compat.jeirouter.terminal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

final class CuriosAccess {
    static int size(Player player) {
        return CuriosApi.getCuriosInventory(player).map(h -> h.getEquippedCurios().getSlots()).orElse(0);
    }
    static ItemStack get(Player player, int slot) {
        return CuriosApi.getCuriosInventory(player).map(h -> {
            var inv = h.getEquippedCurios();
            return slot >= 0 && slot < inv.getSlots() ? inv.getStackInSlot(slot) : ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY);
    }
    static void set(Player player, int slot, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).ifPresent(h -> h.getEquippedCurios().setStackInSlot(slot, stack));
    }
}
