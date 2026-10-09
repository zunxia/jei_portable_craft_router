package compat.jeirouter.terminal;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.me.items.WirelessCraftingTermMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Ae2WirelessCraftingAdapter implements TerminalAdapter {
    public String id() { return "ae2_wireless"; }
    public String itemId() { return "ae2:wireless_crafting_terminal"; }
    protected MenuType<?> menuType() { return WirelessCraftingTermMenu.TYPE; }
    public boolean isSafeCandidate(ItemStack stack) { return SnapshotItemLocator.available() && matches(stack) && stack.getItem() instanceof WirelessTerminalItem; }
    public Component open(ServerPlayer player, ItemReference ref, ItemStack stack) {
        try { return openChecked(player, ref, stack); }
        catch (RuntimeException | LinkageError e) {
            compat.jeirouter.JeiPortableRouter.LOG.warn("AE2 terminal open failed at {}", ref, e);
            return Component.translatable("router.open_failed");
        }
    }
    private Component openChecked(ServerPlayer player, ItemReference ref, ItemStack stack) {
        if (!SnapshotItemLocator.available()) return Component.translatable("router.open_failed");
        if (ref.locatorKind(player) == ItemReference.LocatorKind.UNSAFE || !matches(ref.get(player)) || stack.isEmpty()) return Component.translatable("router.invalid_item");
        if (!(stack.getItem() instanceof WirelessTerminalItem item)) return Component.translatable("router.invalid_item");
        // forStack 在当前 AE2 中没有网络 codec，注册的定位器实现同样的客户端快照语义。
        // 服务端继续定位真实槽位，防止在物品副本上耗电或保存合成格。
        var locator = new SnapshotItemLocator(ref, stack.copy());
        if (item.getLinkedPosition(stack) == null) return Component.translatable("router.not_linked");
        var reason = new Component[] {Component.translatable("router.network_missing")};
        if (item.getLinkedGrid(stack, player.level(), m -> reason[0] = m) == null) return reason[0];
        // 构造 host 本身只模拟空闲耗电，不调用 tick，避免验证时扣电。
        var host = item.getMenuHost(player, locator, null);
        if (host == null) return Component.translatable("router.open_failed");
        var status = host.getLinkStatus();
        if (!status.connected()) {
            Component description = status.statusDescription();
            if (description != null && description.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                if (t.getKey().equals("chat.ae2.OutOfRange")) return Component.translatable("router.out_of_range");
                if (t.getKey().equals("gui.ae2.OutOfPower")) return Component.translatable("router.out_of_power");
            }
            return description != null ? description : Component.translatable("router.open_failed");
        }
        var targetType = menuType();
        // AE2WTLib 会用自己的 ItemWCT 替换 ae2:wireless_crafting_terminal；
        // 此时 registry id 未变，实际 host 已变为 WCTMenuHost，必须跟随物品 opener 的菜单。
        if (net.neoforged.fml.ModList.get().isLoaded("ae2wtlib")) {
            targetType = Ae2WtMenuCompat.menuType(item, locator, player, targetType);
        }
        return MenuOpener.open(targetType, player, locator) ? null : Component.translatable("router.open_failed");
    }
}
