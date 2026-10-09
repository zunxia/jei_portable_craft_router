package compat.jeirouter.client;

import compat.jeirouter.JeiPortableRouter;
import compat.jeirouter.RouterConfig;
import compat.jeirouter.terminal.TerminalPriorityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = JeiPortableRouter.ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!ClientSetup.enabled || !RouterConfig.SPEC.isLoaded()) return;
        try { ClientRouter.tick(); }
        catch (RuntimeException | LinkageError e) { ClientRouter.failSoft(e); }
    }
    @SubscribeEvent public static void screen(ScreenEvent.Opening event) {
        // Loading/error screens can open before COMMON configs have been loaded.
        if (!(event.getNewScreen() instanceof InventoryScreen) || !RouterConfig.SPEC.isLoaded()) return;
        if (RouterConfig.DEBUG.get()) {
            var candidates = TerminalPriorityResolver.find(Minecraft.getInstance().player);
            if (!candidates.isEmpty()) JeiPortableRouter.LOG.info("Inventory terminal candidates: {}", candidates);
        }
    }
}
