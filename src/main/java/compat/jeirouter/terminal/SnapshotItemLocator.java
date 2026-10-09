package compat.jeirouter.terminal;

import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** 客户端用随打开包传入的组件快照；服务端始终使用槽位内的真实物品。 */
public record SnapshotItemLocator(ItemReference reference, ItemStack snapshot) implements ItemMenuHostLocator {
    private static boolean available;
    public static boolean available() { return available; }
    public static void register() {
        MenuLocators.register(SnapshotItemLocator.class, SnapshotItemLocator::write, SnapshotItemLocator::read);
        available = true;
    }
    public ItemStack locateItem(Player player) {
        if (player.level().isClientSide()) return snapshot;
        var actual = reference.get(player);
        return actual.is(snapshot.getItem()) ? actual : ItemStack.EMPTY;
    }
    public BlockHitResult hitResult() { return null; }
    public Integer getPlayerInventorySlot() { return reference.curio() ? null : reference.slot(); }
    private void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(reference.curio());
        buffer.writeVarInt(reference.slot());
        ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, snapshot);
    }
    private static SnapshotItemLocator read(FriendlyByteBuf buffer) {
        var reference = new ItemReference(buffer.readBoolean(), buffer.readVarInt());
        return new SnapshotItemLocator(reference, ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
    }
}
