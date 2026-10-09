package compat.jeirouter.terminal;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.cyclops.cyclopscore.inventory.ItemLocation;
import org.cyclops.cyclopscore.inventory.InventoryLocationPlayer;
import org.cyclops.cyclopscore.modcompat.curios.InventoryLocationCurios;
import org.cyclops.integratedterminals.item.ItemTerminalStoragePortable;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageItem;
import org.cyclops.integratedterminals.core.terminalstorage.TerminalStorageTabIngredientComponentItemStackCrafting;

public final class IntegratedTerminalsAdapter implements TerminalAdapter {
    public String id() { return "integrated"; }
    public String itemId() { return "integratedterminals:terminal_storage_portable"; }
    public Component open(ServerPlayer player, ItemReference ref, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemTerminalStoragePortable item)) return Component.translatable("router.invalid_item");
        if (ItemTerminalStoragePortable.getGroupId(stack) < 0) return Component.translatable("router.not_linked");
        if (ContainerTerminalStorageItem.getNetworkFromItem(stack).isEmpty()) return Component.translatable("router.network_missing");
        var location = new ItemLocation(ref.curio() ? new InventoryLocationCurios() : InventoryLocationPlayer.getInstance(), ref.slot());
        var tabId = TerminalStorageTabIngredientComponentItemStackCrafting.NAME;
        if (tabId == null) return Component.translatable("router.no_crafting_tab");
        var tab = tabId.toString();
        // 在 opener 序列化菜单初始数据之前保存标签页，客户端与服务端初始状态一致。
        ItemTerminalStoragePortable.getTerminalStorageState(stack, player, location).setTab(tab);
        item.openGuiForItemIndex(player.level(), player, location);
        if (!(player.containerMenu instanceof ContainerTerminalStorageItem menu)) return Component.translatable("router.open_failed");
        menu.setSelectedTab(tab);
        return null;
    }
}
