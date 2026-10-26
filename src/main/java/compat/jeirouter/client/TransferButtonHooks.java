package compat.jeirouter.client;

import compat.jeirouter.apply.JeiTransferService;
import compat.jeirouter.apply.MaterialGate;
import compat.jeirouter.apply.RecipeSnapshot;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/** Shared behavior; the mixins only supply the version-specific controller fields. */
public final class TransferButtonHooks {
    public static void update(IRecipeLayoutDrawable<?> layout, AbstractContainerScreen<?> source, IRecipeTransferError error, IButtonState state) {
        try {
            if (ClientRouter.canRoute(layout, source, error)) {
                state.setActive(true); state.setVisible(true);
                var candidate = ClientRouter.candidates().getFirst();
                var player = Minecraft.getInstance().player;
                var runtime = JeiTransferService.runtime();
                if (player != null && runtime.isPresent()) state.setIcon(runtime.get().getJeiHelpers().getGuiHelper()
                    .createDrawableItemStack(candidate.reference().get(player).copy()));
                var materials = MaterialGate.preview(RecipeSnapshot.capture(layout, false), candidate.adapter().id());
                if (materials.verdict() == MaterialGate.Verdict.NOT_ENOUGH) {
                    state.setIcon(runtime.get().getJeiHelpers().getGuiHelper().createDrawableItemStack(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER)));
                }
            } else state.setIcon(Internal.getTextures().getRecipeTransfer());
        } catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); }
    }
    /** null preserves JEI's own click handler. */
    public static Boolean press(IRecipeLayoutDrawable<?> layout, AbstractContainerScreen<?> source, IRecipeTransferError error,
            boolean maxTransfer, IJeiUserInput input) {
        try {
            if (ClientRouter.canRoute(layout, source, error)) {
                if (!input.isSimulate()) ClientRouter.begin(layout, JeiTransferService.current(), maxTransfer,
                    (input.getModifiers() & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL) != 0);
                return true;
            }
        } catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); }
        return null;
    }
    public static boolean tooltip(IRecipeLayoutDrawable<?> layout, AbstractContainerScreen<?> source, IRecipeTransferError error, ITooltipBuilder tooltip) {
        try {
            if (compat.jeirouter.RouterConfig.ENABLED.get() && source != null && error != null &&
                error.getType() == IRecipeTransferError.Type.COSMETIC && net.neoforged.fml.ModList.get().isLoaded("ae2")) {
                var missing = compat.jeirouter.apply.Ae2MaterialGate.ingredients(source.getMenu(), RecipeSnapshot.capture(layout, false));
                if (missing != null && missing.anyCraftable()) {
                    tooltip.add(appeng.core.localization.ItemModText.MOVE_ITEMS.text());
                    tooltip.add(Component.translatable("router.ctrl_craft").withStyle(net.minecraft.ChatFormatting.BLUE));
                    if (missing.anyMissing()) tooltip.add(appeng.core.localization.ItemModText.MISSING_ITEMS.text().withStyle(net.minecraft.ChatFormatting.RED));
                    return true;
                }
            }
            if (ClientRouter.isOversizedInventoryRecipe(layout, source, error)) {
                var candidates = ClientRouter.candidates();
                if (!candidates.isEmpty()) {
                    if (ClientRouter.serverSupported()) {
                        tooltip.add(Component.translatable("router.transfer_to", candidates.getFirst().name()));
                        var gate = MaterialGate.preview(RecipeSnapshot.capture(layout, false), candidates.getFirst().adapter().id());
                        tooltip.add(switch (gate.verdict()) {
                            case ENOUGH -> Component.translatable("router.material_ready");
                            case CRAFTABLE -> Component.translatable("router.ctrl_craft").withStyle(net.minecraft.ChatFormatting.BLUE);
                            case NOT_ENOUGH -> Component.translatable("router.material_missing", gate.missingCount());
                            case UNKNOWN -> Component.translatable("router.material_check_on_open");
                        });
                        return true;
                    } else tooltip.add(Component.translatable("router.server_jei"));
                }
            }
        } catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); }
        return false;
    }
    private TransferButtonHooks() {}
}
