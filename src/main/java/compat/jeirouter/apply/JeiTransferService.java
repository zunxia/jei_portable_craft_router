package compat.jeirouter.apply;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Delegates to each supported JEI version's own transfer implementation. */
public final class JeiTransferService {
    private static final Class<?> MODERN = modernClass();
    private static final Method ERROR = method("getTransferRecipeError", false);
    private static final Method TRANSFER = method("transferRecipe", true);
    private static final CustomPacketPayload.Type<?> CHANNEL = channel();
    private final IRecipeTransferManager manager;
    private final Object delegate;

    public JeiTransferService(IRecipeTransferManager manager) {
        this.manager = manager;
        try { delegate = MODERN == null ? null : MODERN.getConstructor(IRecipeTransferManager.class).newInstance(manager); }
        catch (ReflectiveOperationException e) { throw failure(e); }
    }
    public static JeiTransferService current() { return new JeiTransferService(Internal.getJeiRuntime().getRecipeTransferManager()); }
    public static Optional<IJeiRuntime> runtime() {
        // getOptionalJeiRuntime was added after the first controller-based JEI releases.
        try { return Optional.ofNullable(Internal.getJeiRuntime()); }
        catch (IllegalStateException e) { return Optional.empty(); }
    }
    public Optional<IRecipeTransferError> getTransferRecipeError(AbstractContainerScreen<?> screen, IRecipeLayoutDrawable<?> layout, Player player) {
        @SuppressWarnings("unchecked")
        var result = (Optional<IRecipeTransferError>) invoke(ERROR, screen, layout, player, false);
        return result;
    }
    public boolean transferRecipe(AbstractContainerScreen<?> screen, IRecipeLayoutDrawable<?> layout, Player player, boolean maxTransfer) {
        return (Boolean) invoke(TRANSFER, screen, layout, player, maxTransfer);
    }
    public static AbstractContainerScreen<?> parentScreen(RecipesGui gui) {
        if (gui == null) return null;
        var parent = gui.getParentScreen().orElse(null);
        return parent instanceof AbstractContainerScreen<?> container ? container : null;
    }
    public static boolean serverSupported() {
        var connection = Internal.getServerConnection();
        if (connection == null || !connection.isJeiOnServer()) return false;
        var client = net.minecraft.client.Minecraft.getInstance().getConnection();
        // Older JEI connections expose only isJeiOnServer; NeoForge supplies the actual channel handshake.
        return client != null && client.hasChannel(CHANNEL);
    }
    private Object invoke(Method method, AbstractContainerScreen<?> screen, IRecipeLayoutDrawable<?> layout, Player player, boolean maxTransfer) {
        Object[] args = MODERN == null
            ? (method == ERROR ? new Object[] {manager, screen.getMenu(), layout, player} : new Object[] {manager, screen.getMenu(), layout, player, maxTransfer})
            : (method == ERROR ? new Object[] {screen, layout, player} : new Object[] {screen, layout, player, maxTransfer});
        try { return method.invoke(delegate, args); }
        catch (ReflectiveOperationException e) { throw failure(e); }
    }
    private static Class<?> modernClass() {
        try { return Class.forName("mezz.jei.common.transfer.RecipeTransferService"); }
        catch (ClassNotFoundException e) { return null; }
    }
    private static CustomPacketPayload.Type<?> channel() {
        try {
            String name = MODERN == null ? "PacketRecipeTransfer" : "PacketRecipeTransferWithResult";
            return (CustomPacketPayload.Type<?>) Class.forName("mezz.jei.common.network.packets." + name).getField("TYPE").get(null);
        } catch (ReflectiveOperationException e) { throw failure(e); }
    }
    private static Method method(String name, boolean transfer) {
        try {
            Class<?> owner = MODERN == null ? Class.forName("mezz.jei.common.transfer.RecipeTransferUtil") : MODERN;
            Class<?>[] args = MODERN == null
                ? (transfer ? new Class<?>[] {IRecipeTransferManager.class, AbstractContainerMenu.class, IRecipeLayoutDrawable.class, Player.class, boolean.class}
                            : new Class<?>[] {IRecipeTransferManager.class, AbstractContainerMenu.class, IRecipeLayoutDrawable.class, Player.class})
                : (transfer ? new Class<?>[] {AbstractContainerScreen.class, IRecipeLayoutDrawable.class, Player.class, boolean.class}
                            : new Class<?>[] {AbstractContainerScreen.class, IRecipeLayoutDrawable.class, Player.class});
            return owner.getMethod(name, args);
        } catch (ReflectiveOperationException e) { throw failure(e); }
    }
    private static RuntimeException failure(ReflectiveOperationException e) {
        if (e instanceof InvocationTargetException invocation) {
            if (invocation.getCause() instanceof RuntimeException runtime) return runtime;
            if (invocation.getCause() instanceof Error error) throw error;
        }
        return new IllegalStateException("Supported JEI transfer API unavailable", e);
    }
}
