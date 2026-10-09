package compat.jeirouter.terminal;

import de.mari_023.ae2wtlib.wet.WETMenu;
import net.minecraft.world.inventory.MenuType;

// AE2WTLib 的无线样板编码终端；复用真实物品校验、快照定位与原 opener。
public class Ae2PatternTerminalAdapter extends Ae2WirelessCraftingAdapter {
    public String id() { return "ae2_pattern"; }
    public String itemId() { return "ae2wtlib:wireless_pattern_encoding_terminal"; }
    protected MenuType<?> menuType() { return WETMenu.TYPE; }
}
