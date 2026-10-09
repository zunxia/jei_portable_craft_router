package compat.jeirouter.client;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import compat.jeirouter.apply.JeiTransferService;
import mezz.jei.gui.recipes.RecipesGui;

public interface PatternButtonHost {
    void router$addPattern(IRecipeLayoutDrawable<?> layout, RecipesGui gui, JeiTransferService service);
}
