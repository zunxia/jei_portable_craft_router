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
        var jei = net.neoforged.fml.ModList.get().getModContainerById("jei");
        enabled = jei.map(c -> compat.jeirouter.JeiVersionSupport.supported(c.getModInfo().getVersion().toString())).orElse(false);
        if (enabled) {
            try { RouterNetwork.clientReceiver = ClientRouter::receive; }
            catch (LinkageError e) { enabled = false; JeiPortableRouter.LOG.warn("JEI client API unavailable; routing disabled", e); }
        } else JeiPortableRouter.LOG.warn("Supported JEI ({}) unavailable; client routing disabled", compat.jeirouter.JeiVersionSupport.DESCRIPTION);
    }
}
