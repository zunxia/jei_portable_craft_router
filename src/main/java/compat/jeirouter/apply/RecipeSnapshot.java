package compat.jeirouter.apply;

import java.util.List;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

// 保留点击时的布局与槽位视图，菜单打开后由现有 JEI handler 对新菜单重算槽位。
// 配方对象仅在客户端持有，不跨网络传输；id 用于确认配方没有被替换。
public record RecipeSnapshot(ResourceLocation recipeId, IRecipeLayoutDrawable<?> layout,
        List<IRecipeSlotView> inputs, boolean maxTransfer) {
    @SuppressWarnings("unchecked")
    public RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> craftingRecipe() {
        return layout.getRecipe() instanceof RecipeHolder<?> holder && holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe
            ? (RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>) holder : null;
    }
    public static RecipeSnapshot capture(IRecipeLayoutDrawable<?> layout, boolean maxTransfer) {
        var id = layout.getRecipe() instanceof RecipeHolder<?> holder ? holder.id() : layout.getRecipeCategory().getRecipeType().getUid();
        return new RecipeSnapshot(id, layout,
            List.copyOf(layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT)), maxTransfer);
    }
}
