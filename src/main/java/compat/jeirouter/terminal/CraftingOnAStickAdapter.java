package compat.jeirouter.terminal;

import com.ofekn.crafting_on_a_stick.ItemOnAStick;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;

public final class CraftingOnAStickAdapter implements TerminalAdapter {
    public String id() { return "stick"; }
    public String itemId() { return "crafting_on_a_stick:crafting_table"; }
    public Component open(ServerPlayer player, ItemReference ref, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemOnAStick item)) return Component.translatable("router.invalid_item");
        var result = item.openContainer(player, stack);
        if (result != stack) ref.set(player, result);
        return player.containerMenu instanceof CraftingMenu ? null : Component.translatable("router.open_failed");
    }
}
