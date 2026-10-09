package compat.jeirouter.apply;

import compat.jeirouter.JeiPortableRouter;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class RouterJeiPlugin implements IModPlugin {
    private static IRecipeTransferHandlerHelper helper;
    public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(JeiPortableRouter.ID, "materials"); }
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        try { helper = registration.getTransferHelper(); }
        catch (RuntimeException | LinkageError e) {
            helper = null;
            JeiPortableRouter.LOG.warn("JEI material helper unavailable; automatic apply will be blocked", e);
        }
    }
    public static IRecipeTransferHandlerHelper transferHelper() { return helper; }
}
