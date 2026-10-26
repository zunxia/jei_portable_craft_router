package compat.jeirouter;

import compat.jeirouter.network.RouterNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(JeiPortableRouter.ID)
public final class JeiPortableRouter {
    public static final String ID = "jei_portable_craft_router";
    public static final Logger LOG = LoggerFactory.getLogger(ID);
    public JeiPortableRouter(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RouterConfig.SPEC);
        bus.addListener(RouterNetwork::register);
        if (net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            try { compat.jeirouter.terminal.SnapshotItemLocator.register(); }
            catch (RuntimeException | LinkageError e) { LOG.warn("AE2 snapshot locator unavailable; AE2 routing disabled", e); }
        }
        LOG.info("{} 1.3.5 loaded; server-authoritative portable crafting routing", ID);
    }
}
