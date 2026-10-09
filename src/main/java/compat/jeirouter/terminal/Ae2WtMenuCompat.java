package compat.jeirouter.terminal;

import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

final class Ae2WtMenuCompat {
    static MenuType<?> menuType(WirelessTerminalItem item, ItemMenuHostLocator locator, Player player, MenuType<?> fallback) {
        return item instanceof ItemWT terminal ? terminal.getMenuType(locator, player) : fallback;
    }
}
