package compat.jeirouter.mixin;

import compat.jeirouter.client.ClientRouter;
import compat.jeirouter.client.PatternButtonController;
import compat.jeirouter.client.PatternButtonHost;
import compat.jeirouter.apply.JeiTransferService;
import java.util.List;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import mezz.jei.gui.recipes.RecipesGui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import mezz.jei.common.util.ImmutableRect2i;

@Mixin(targets = "mezz.jei.gui.recipes.RecipeLayoutWithButtons", remap = false)
public abstract class RecipeLayoutWithButtonsMixin implements PatternButtonHost {
    @Shadow @Final private List<IconButton> buttons;
    @Shadow @Final private IRecipeLayoutDrawable<?> recipeLayout;
    @Inject(method = "create", at = @At("RETURN"), require = 0, remap = false)
    private static <T> void router$create(IRecipeLayoutDrawable<T> layout, RecipeBookmark<?, ?> bookmark, BookmarkList bookmarks,
            RecipesGui gui, @Coerce Object service, List<IRecipeButtonControllerFactory> factories,
            CallbackInfoReturnable<IRecipeLayoutWithButtons<T>> cir) {
        try {
            if (cir.getReturnValue() instanceof PatternButtonHost host) host.router$addPattern(layout, gui, JeiTransferService.current());
        } catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); }
    }
    @Unique public void router$addPattern(IRecipeLayoutDrawable<?> layout, RecipesGui gui, JeiTransferService service) {
        // JEI 按钮原本从下向上排列，位置注入为这两个按钮交换行，收藏仍排在上方。
        buttons.add(Math.min(1, buttons.size()), new IconButton(new PatternButtonController(layout, gui, service)));
    }
    @Inject(method = "getAbsoluteButtonArea", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void router$position(int index, CallbackInfoReturnable<ImmutableRect2i> cir) {
        try {
            if (index > 1 || buttons.size() < 2 || !buttons.get(0).isVisible() || !buttons.get(1).isVisible()) return;
            var anchor = recipeLayout.getSideButtonArea(0);
            var recipe = recipeLayout.getRect();
            cir.setReturnValue(new ImmutableRect2i(recipe.getX() + anchor.getX(), recipe.getY() + anchor.getY() -
                (index == 0 ? anchor.getHeight() + 2 : 0), anchor.getWidth(), anchor.getHeight()));
        } catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); }
    }
}
