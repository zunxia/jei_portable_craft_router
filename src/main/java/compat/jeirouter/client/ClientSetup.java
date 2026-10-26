package compat.jeirouter.client;

import compat.jeirouter.JeiPortableRouter;
import compat.jeirouter.network.RouterNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = JeiPortableRouter.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    static volatile boolean enabled;
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        enabled = net.neoforged.fml.ModList.get().isLoaded("jei");
        if (enabled) {
            try { RouterNetwork.clientReceiver = ClientRouter::receive; }
            catch (LinkageError e) { enabled = false; JeiPortableRouter.LOG.warn("JEI client API unavailable; routing disabled", e); }
        } else JeiPortableRouter.LOG.info("JEI not installed; client routing unavailable");
    }
}
