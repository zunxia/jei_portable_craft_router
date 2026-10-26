package compat.jeirouter.terminal;

import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** 客户端用随打开包传入的组件快照；服务端始终使用槽位内的真实物品。 */
public final class SnapshotItemLocator implements ItemMenuHostLocator {
    private final ItemReference reference;
    private final ItemStack snapshot;
    private Player serverPlayer;
    private static boolean available;
    public SnapshotItemLocator(ItemReference reference, ItemStack snapshot) {
        this.reference = reference;
        this.snapshot = snapshot;
    }
    public ItemReference reference() { return reference; }
    public ItemStack snapshot() { return snapshot; }
    @Override public String toString() { return "SnapshotItemLocator[reference=" + reference + ", snapshot=" + snapshot + "]"; }
    public static boolean available() { return available; }
    public static void register() {
        MenuLocators.register(SnapshotItemLocator.class, SnapshotItemLocator::write, SnapshotItemLocator::read);
        available = true;
    }
    public ItemStack locateItem(Player player) {
        if (player.level().isClientSide()) return snapshot;
        serverPlayer = player;
        var actual = reference.get(player);
        return actual.is(snapshot.getItem()) ? actual : ItemStack.EMPTY;
    }
    public BlockHitResult hitResult() { return null; }
    public Integer getPlayerInventorySlot() { return reference.curio() ? null : reference.slot(); }
    private void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(reference.curio());
        buffer.writeVarInt(reference.slot());
        // AE2WTLib reuses the locator when switching WUT modes or returning from a submenu.
        // Its real item has changed since the initial open: send that item's current components.
        // Client-decoded locators retain packet snapshots; never use client inventory sync timing.
        var current = serverPlayer == null ? snapshot : locateItem(serverPlayer);
        ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, current);
    }
    private static SnapshotItemLocator read(FriendlyByteBuf buffer) {
        var reference = new ItemReference(buffer.readBoolean(), buffer.readVarInt());
        return new SnapshotItemLocator(reference, ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
    }
}
