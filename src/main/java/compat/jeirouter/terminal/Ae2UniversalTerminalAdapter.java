package compat.jeirouter.terminal;

import de.mari_023.ae2wtlib.wct.WCTMenu;
import net.minecraft.world.inventory.MenuType;

public final class Ae2UniversalTerminalAdapter extends Ae2WirelessCraftingAdapter {
    public String id() { return "ae2_universal"; }
    public String itemId() { return "ae2wtlib:wireless_universal_terminal"; }
    protected MenuType<?> menuType() { return WCTMenu.TYPE; }
    public boolean isSafeCandidate(net.minecraft.world.item.ItemStack stack) {
        return super.isSafeCandidate(stack) && stack.has(de.mari_023.ae2wtlib.wut.WTDefinitions.CRAFTING.componentType());
    }
    public net.minecraft.network.chat.Component open(net.minecraft.server.level.ServerPlayer player, ItemReference ref, net.minecraft.world.item.ItemStack stack) {
        var definition = de.mari_023.ae2wtlib.wut.WTDefinitions.CRAFTING;
        if (!stack.has(definition.componentType())) return net.minecraft.network.chat.Component.translatable("router.no_crafting_module");
        // 通用终端可能上次选中的是样板终端；只切换已安装的合成模块，不凭空添加模块。
        var effective = stack.copy();
        effective.set(de.mari_023.ae2wtlib.api.AE2wtlibComponents.CURRENT_TERMINAL, definition);
        if (!net.minecraft.world.item.ItemStack.matches(ref.get(player), stack)) return net.minecraft.network.chat.Component.translatable("router.invalid_item");
        ref.set(player, effective);
        return super.open(player, ref, effective);
    }
}
