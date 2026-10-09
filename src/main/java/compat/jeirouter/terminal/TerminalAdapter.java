package compat.jeirouter.terminal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public interface TerminalAdapter {
    String id();
    String itemId();
    default boolean matches(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId());
    }
    default boolean isSafeCandidate(ItemStack stack) { return matches(stack); }
    // 只在服务端调用；返回 null 表示成功，失败时保留模组原始原因。
    Component open(ServerPlayer player, ItemReference reference, ItemStack stack);
}
