package compat.jeirouter.client;

import compat.jeirouter.RouterConfig;
import compat.jeirouter.terminal.TerminalPriorityResolver;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.buttons.IIconButtonController;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.common.Internal;
import compat.jeirouter.apply.JeiTransferService;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import appeng.menu.me.items.PatternEncodingTermMenu;

public final class PatternButtonController implements IIconButtonController {
    private final IRecipeLayoutDrawable<?> layout;
    private final RecipesGui gui;
    private final JeiTransferService service;
    public PatternButtonController(IRecipeLayoutDrawable<?> layout, RecipesGui gui, JeiTransferService service) {
        this.layout = layout; this.gui = gui; this.service = service;
    }
    public void updateState(IButtonState state) {
        state.setVisible(false); state.setActive(false);
        try {
            if (!RouterConfig.ENABLED.get() || !RouterConfig.PATTERN.get() || !net.neoforged.fml.ModList.get().isLoaded("ae2wtlib")) return;
            // AE2 样板编码界面已有原生“编写样板”按钮，隐藏 JEI 侧重复按钮。
            if (nativePatternScreen()) return;
            boolean visible = !TerminalPriorityResolver.findPatterns(Minecraft.getInstance().player).isEmpty();
            state.setVisible(visible);
            state.setActive(visible && ClientRouter.idle() && ClientRouter.patternSupported() && JeiTransferService.parentScreen(gui) != null);
            if (visible) state.setIcon(new Icon(Internal.getJeiRuntime().getJeiHelpers().getGuiHelper().createDrawableItemStack(
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:blank_pattern"))))));
        } catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); }
    }
    public void getTooltips(ITooltipBuilder tooltip) {
        tooltip.add(Component.translatable("router.write_pattern"));
        if (!ClientRouter.patternSupported()) tooltip.add(Component.translatable("router.pattern_server"));
        else tooltip.add(Component.translatable("router.pattern_hint"));
    }
    public boolean onPress(IJeiUserInput input) {
        try {
            if (nativePatternScreen() || !ClientRouter.idle() || !ClientRouter.patternSupported() || TerminalPriorityResolver.findPatterns(Minecraft.getInstance().player).isEmpty()) return false;
            if (!input.isSimulate()) {
                var screen = JeiTransferService.parentScreen(gui);
                if (PatternRecipeSupport.supports(screen)) {
                    if (PatternRecipeSupport.apply(layout, service, screen)) gui.onClose();
                    else Minecraft.getInstance().player.displayClientMessage(Component.translatable("router.jei_cannot_apply"), false);
                } else ClientRouter.beginPattern(layout, service);
            }
            return true;
        } catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); return false; }
    }
    public boolean nativePatternScreen() {
        AbstractContainerScreen<?> parent = gui == null ? null : JeiTransferService.parentScreen(gui);
        return parent != null && parent.getMenu() instanceof PatternEncodingTermMenu;
    }
    // JEI 的小按钮小于原物品图标；缩放到边框内，保留原按钮底图。
    private record Icon(mezz.jei.api.gui.drawable.IDrawable item) implements mezz.jei.api.gui.drawable.IDrawable {
        public int getWidth() { return 10; }
        public int getHeight() { return 10; }
        public void draw(net.minecraft.client.gui.GuiGraphics graphics, int x, int y) {
            var pose = graphics.pose();
            pose.pushPose();
            try { pose.translate(x, y, 0); pose.scale(10f / item.getWidth(), 10f / item.getHeight(), 1); item.draw(graphics, 0, 0); }
            finally { pose.popPose(); }
        }
    }
}
