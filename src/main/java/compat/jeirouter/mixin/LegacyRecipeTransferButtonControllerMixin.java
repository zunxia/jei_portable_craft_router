package compat.jeirouter.mixin;

import compat.jeirouter.apply.JeiTransferService;
import compat.jeirouter.client.TransferButtonHooks;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mezz.jei.gui.recipes.RecipeTransferButtonController", remap = false)
public abstract class LegacyRecipeTransferButtonControllerMixin {
    @Shadow @Final private IRecipeLayoutDrawable<?> recipeLayout;
    @Shadow @Final private RecipesGui recipesGui;
    @Shadow private IRecipeTransferError recipeTransferError;
    @Inject(method = "updateState", at = @At("RETURN"), require = 0, remap = false)
    private void router$state(IButtonState state, CallbackInfo ci) {
        TransferButtonHooks.update(recipeLayout, JeiTransferService.parentScreen(recipesGui), recipeTransferError, state);
    }
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void router$press(IJeiUserInput input, CallbackInfoReturnable<Boolean> cir) {
        var result = TransferButtonHooks.press(recipeLayout, JeiTransferService.parentScreen(recipesGui), recipeTransferError, Screen.hasShiftDown(), input);
        if (result != null) cir.setReturnValue(result);
    }
    @Inject(method = "getTooltips", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void router$tooltip(ITooltipBuilder tooltip, CallbackInfo ci) {
        if (TransferButtonHooks.tooltip(recipeLayout, JeiTransferService.parentScreen(recipesGui), recipeTransferError, tooltip)) ci.cancel();
    }
}
