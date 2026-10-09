package compat.jeirouter.terminal;

import de.mari_023.ae2wtlib.api.AE2wtlibComponents;
import de.mari_023.ae2wtlib.wut.WTDefinitions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class Ae2UniversalPatternAdapter extends Ae2PatternTerminalAdapter {
    public String id() { return "ae2_universal_pattern"; }
    public String itemId() { return "ae2wtlib:wireless_universal_terminal"; }
    public boolean isSafeCandidate(ItemStack stack) {
        return super.isSafeCandidate(stack) && stack.has(WTDefinitions.PATTERN_ENCODING.componentType());
    }
    public Component open(ServerPlayer player, ItemReference ref, ItemStack stack) {
        if (!stack.has(WTDefinitions.PATTERN_ENCODING.componentType())) return Component.translatable("router.no_pattern_module");
        var effective = stack.copy();
        effective.set(AE2wtlibComponents.CURRENT_TERMINAL, WTDefinitions.PATTERN_ENCODING);
        if (!ItemStack.matches(ref.get(player), stack)) return Component.translatable("router.invalid_item");
        ref.set(player, effective);
        return super.open(player, ref, effective);
    }
}
