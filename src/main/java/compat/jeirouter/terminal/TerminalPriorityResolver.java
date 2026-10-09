package compat.jeirouter.terminal;

import compat.jeirouter.JeiPortableRouter;
import compat.jeirouter.RouterConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class TerminalPriorityResolver {
    public record Candidate(TerminalAdapter adapter, ItemReference reference, Component name) {}
    public static List<TerminalAdapter> adapters() {
        var mods = ModList.get();
        var list = new ArrayList<TerminalAdapter>();
        for (String id : RouterConfig.PRIORITY.get().stream().distinct().toList()) {
            try {
                switch (id) {
                    case "ae2_wireless" -> { if (RouterConfig.AE2.get() && mods.isLoaded("ae2")) list.add(new Ae2WirelessCraftingAdapter()); }
                    case "ae2_universal" -> { if (RouterConfig.UNIVERSAL.get() && mods.isLoaded("ae2") && mods.isLoaded("ae2wtlib")) list.add(new Ae2UniversalTerminalAdapter()); }
                    case "integrated" -> { if (RouterConfig.INTEGRATED.get() && mods.isLoaded("integratedterminals")) list.add(new IntegratedTerminalsAdapter()); }
                    case "stick" -> { if (RouterConfig.STICK.get() && mods.isLoaded("crafting_on_a_stick")) list.add(new CraftingOnAStickAdapter()); }
                    default -> { }
                }
            } catch (LinkageError e) { JeiPortableRouter.LOG.warn("Unavailable terminal adapter {}", id, e); }
        }
        return List.copyOf(list);
    }
    public static List<Candidate> find(Player player) {
        return find(player, adapters());
    }
    public static List<TerminalAdapter> patternAdapters() {
        var mods = ModList.get();
        if (!RouterConfig.ENABLED.get() || !RouterConfig.PATTERN.get() || !mods.isLoaded("ae2") || !mods.isLoaded("ae2wtlib")) return List.of();
        try { return List.of(new Ae2PatternTerminalAdapter(), new Ae2UniversalPatternAdapter()); }
        catch (LinkageError e) { JeiPortableRouter.LOG.warn("Pattern adapters unavailable", e); return List.of(); }
    }
    public static List<Candidate> findPatterns(Player player) { return find(player, patternAdapters()); }
    private static List<Candidate> find(Player player, List<TerminalAdapter> adapters) {
        if (player == null || !RouterConfig.ENABLED.get()) return List.of();
        var refs = new ArrayList<ItemReference>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) refs.add(new ItemReference(false, i));
        if (ModList.get().isLoaded("curios")) {
            for (int i = 0; i < CuriosAccess.size(player); i++) refs.add(new ItemReference(true, i));
        }
        var result = new ArrayList<Candidate>();
        for (var adapter : adapters) for (var ref : refs) {
            var stack = ref.get(player);
            try {
                if (adapter.isSafeCandidate(stack) && ref.locatorKind(player) != ItemReference.LocatorKind.UNSAFE) {
                    result.add(new Candidate(adapter, ref, stack.getHoverName().copy()));
                }
            } catch (RuntimeException | LinkageError e) { JeiPortableRouter.LOG.debug("Unsafe terminal candidate at {}", ref, e); }
        }
        return List.copyOf(result);
    }
    private TerminalPriorityResolver() {}
}
