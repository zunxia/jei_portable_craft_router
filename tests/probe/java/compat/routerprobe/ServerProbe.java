package compat.routerprobe;

import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import appeng.menu.me.items.CraftingTermMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "router_probe", value = Dist.DEDICATED_SERVER)
public final class ServerProbe {
    private static final HashSet<UUID> verified = new HashSet<>();
    private static int completeTicks;
    private static final boolean AE2_CURIO = Boolean.getBoolean("router.probe.remoteAe2");
    private static final boolean EMPTY = Boolean.getBoolean("router.probe.empty");
    private static final boolean PATTERN = Boolean.getBoolean("router.probe.pattern");
    private static final java.util.Map<UUID, Integer> openTicks = new java.util.HashMap<>();
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("crafting_on_a_stick:crafting_table"))));
        if (!EMPTY && !PATTERN) player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 64));
        if (AE2_CURIO || PATTERN) {
            player.getInventory().setItem(0, ItemStack.EMPTY);
            var energyPos = new net.minecraft.core.BlockPos(4, -59, 4);
            var accessPos = energyPos.above();
            player.teleportTo(0.5, -58, 0.5);
            player.serverLevel().setBlockAndUpdate(energyPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")).defaultBlockState());
            player.serverLevel().setBlockAndUpdate(accessPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:wireless_access_point")).defaultBlockState());
            appeng.api.orientation.BlockOrientation.UP_NORTH.setOn(player.serverLevel(), accessPos);
            var terminal = de.mari_023.ae2wtlib.wut.WTDefinitions.CRAFTING.universalTerminal().copy();
            terminal.set(de.mari_023.ae2wtlib.wut.WTDefinitions.PATTERN_ENCODING.componentType(), com.mojang.datafixers.util.Unit.INSTANCE);
            terminal.set(de.mari_023.ae2wtlib.api.AE2wtlibComponents.CURRENT_TERMINAL, PATTERN ? de.mari_023.ae2wtlib.wut.WTDefinitions.CRAFTING : de.mari_023.ae2wtlib.wut.WTDefinitions.PATTERN_ENCODING);
            var item = (appeng.items.tools.powered.WirelessTerminalItem) terminal.getItem();
            item.injectAEPower(terminal, item.getAEMaxPower(terminal), appeng.api.config.Actionable.MODULATE);
            appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER.link(terminal, net.minecraft.core.GlobalPos.of(player.level().dimension(), accessPos));
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow().getEquippedCurios().setStackInSlot(0, terminal);
        }
        player.inventoryMenu.broadcastChanges();
        LoggerFactory.getLogger("router_probe").info("PROBE READY: {}", player.getName().getString());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            if (PATTERN) {
                if (!verified.contains(player.getUUID()) && player.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu pattern &&
                    java.util.Arrays.stream(pattern.getCraftingGridSlots()).filter(s -> s.getItem().is(Items.OAK_PLANKS)).count() == 8 &&
                    player.getInventory().countItem(Items.OAK_PLANKS) == 0) {
                    verified.add(player.getUUID());
                    LoggerFactory.getLogger("router_probe").info("PROBE PASS: dedicated pattern route wrote eight ghost inputs for {} ({})", player.getName().getString(), player.getUUID());
                }
                continue;
            }
            if (verified.contains(player.getUUID()) || !(player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof CraftingTermMenu)) continue;
            var menu = player.containerMenu;
            int filled = 0;
            for (var slot : menu.slots) if (slot.container != player.getInventory() && slot.getItem().is(Items.OAK_PLANKS)) filled++;
            boolean output = menu.slots.stream().anyMatch(s -> s.container != player.getInventory() && s.getItem().is(Items.CHEST));
            if (EMPTY && openTicks.merge(player.getUUID(), 1, Integer::sum) >= 40 && filled == 0 && !output) {
                verified.add(player.getUUID());
                LoggerFactory.getLogger("router_probe").info("PROBE PASS: dedicated material gate preserved empty grid for {}", player.getName().getString());
            } else if (!EMPTY && filled == 8 && output) {
                verified.add(player.getUUID());
                LoggerFactory.getLogger("router_probe").info("PROBE PASS: dedicated server filled 3x3 and produced chest for {} ({})", player.getName().getString(), player.getUUID());
            }
        }
        if (verified.size() >= 2 && ++completeTicks == 160) {
            LoggerFactory.getLogger("router_probe").info("PROBE PASS: TWO DISTINCT REMOTE PLAYERS COMPLETE");
            event.getServer().halt(false);
        }
    }
}
