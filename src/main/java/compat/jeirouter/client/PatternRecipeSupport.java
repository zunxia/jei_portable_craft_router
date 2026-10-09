package compat.jeirouter.client;

import appeng.menu.me.items.PatternEncodingTermMenu;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.common.Internal;
import compat.jeirouter.apply.JeiTransferService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class PatternRecipeSupport {
    public static boolean supports(AbstractContainerScreen<?> screen) {
        return screen != null && screen.getMenu() instanceof PatternEncodingTermMenu;
    }
    public static boolean apply(IRecipeLayoutDrawable<?> layout, JeiTransferService service, AbstractContainerScreen<?> screen) {
        var player = Minecraft.getInstance().player;
        if (player == null || !supports(screen) || player.containerMenu != screen.getMenu()) return false;
        var error = service.getTransferRecipeError(screen, layout, player);
        if (error.isPresent() && !error.get().getType().allowsTransfer) return false;
        // 样板使用虚拟槽位，不以现货材料预检阻止编写；不消耗配方原料或自动领取编码样板。
        return service.transferRecipe(screen, layout, player, false);
    }
    private PatternRecipeSupport() {}
}
