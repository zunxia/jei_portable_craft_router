import compat.jeirouter.mixin.RouterMixinPlugin;
import compat.jeirouter.network.OpenTerminalRequestPayload;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public final class RouterChecks {
    public static void main(String[] args) throws Exception {
        check(!compat.jeirouter.RouterConfig.SPEC.isLoaded(), "startup fixture has no loaded config");
        boolean rejectsEarlyRead = false;
        try { compat.jeirouter.RouterConfig.DEBUG.get(); }
        catch (IllegalStateException expected) { rejectsEarlyRead = true; }
        check(rejectsEarlyRead, "NeoForge rejects the original early config read");
        var probeScreen = new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("probe")) {};
        compat.jeirouter.client.ClientEvents.screen(new net.neoforged.neoforge.client.event.ScreenEvent.Opening(probeScreen, probeScreen));
        check(!compat.jeirouter.RouterConfig.SPEC.isLoaded(), "opening a screen before config load no longer crashes");
        check(compat.jeirouter.JeiVersionSupport.supported("19.50.0.414"), "ATM10 8.1 JEI version accepted");
        check(compat.jeirouter.JeiVersionSupport.supported("19.57.0.446"), "ATM10 8.2 JEI version accepted");
        check(!compat.jeirouter.JeiVersionSupport.supported(null) && !compat.jeirouter.JeiVersionSupport.supported("19.58.0.0"),
            "missing or unvalidated JEI version rejected");
        ClassNode original = null;
        for (Path jar : Files.list(Path.of(args[0], "cp")).filter(p -> p.toString().endsWith(".jar")).toList()) {
            try (var z = new ZipFile(jar.toFile())) {
                var entry = z.getEntry("mezz/jei/gui/recipes/RecipeTransferButtonController.class");
                if (entry != null) {
                    original = new ClassNode();
                    new ClassReader(z.getInputStream(entry)).accept(original, 0);
                }
            }
        }
        check(original != null && RouterMixinPlugin.signatureMatches(original), "actual JEI controller is compatible");
        boolean legacy = original.fields.stream().anyMatch(f -> f.name.equals("recipesGui"));
        check(RouterMixinPlugin.signatureMatches(original, legacy) && !RouterMixinPlugin.signatureMatches(original, !legacy),
            "only matching controller adapter is compatible");
        String[] shadows = legacy ? new String[] {"recipeLayout", "recipesGui", "recipeTransferError"}
            : new String[] {"recipeLayout", "recipeTransferService", "screenSupplier", "maxTransferSupplier", "recipeTransferError"};
        for (String name : shadows) {
            var node = copy(original);
            node.fields.removeIf(f -> f.name.equals(name));
            check(!RouterMixinPlugin.signatureMatches(node), "missing shadow disables mixin: " + name);
        }
        for (String name : new String[] {"updateState", "onPress", "getTooltips"}) {
            var node = copy(original);
            node.methods.stream().filter(m -> m.name.equals(name)).forEach(m -> m.desc = "()V");
            check(!RouterMixinPlugin.signatureMatches(node), "changed method disables mixin: " + name);
        }
        ClassNode layout = null;
        for (Path jar : Files.list(Path.of(args[0], "cp")).filter(p -> p.toString().endsWith(".jar")).toList()) {
            try (var z = new ZipFile(jar.toFile())) {
                var entry = z.getEntry("mezz/jei/gui/recipes/RecipeLayoutWithButtons.class");
                if (entry != null) { layout = new ClassNode(); new ClassReader(z.getInputStream(entry)).accept(layout, 0); }
            }
        }
        check(layout != null && RouterMixinPlugin.signatureMatches(layout), "actual JEI button layout is compatible");
        check(RouterMixinPlugin.signatureMatches(layout, legacy) && !RouterMixinPlugin.signatureMatches(layout, !legacy),
            "only matching layout adapter is compatible");
        var withoutButtons = copy(layout);
        withoutButtons.fields.removeIf(f -> f.name.equals("buttons"));
        check(!RouterMixinPlugin.signatureMatches(withoutButtons), "missing button list disables layout mixin");
        var changedFactory = copy(layout);
        changedFactory.methods.stream().filter(m -> m.name.equals("create")).forEach(m -> m.desc = "()V");
        check(!RouterMixinPlugin.signatureMatches(changedFactory), "changed layout factory disables pattern mixin");
        var changedArea = copy(layout);
        changedArea.methods.stream().filter(m -> m.name.equals("getAbsoluteButtonArea")).forEach(m -> m.desc = "()V");
        check(!RouterMixinPlugin.signatureMatches(changedArea), "changed button positioning disables pattern mixin");
        var withoutRecipe = copy(layout);
        withoutRecipe.fields.removeIf(f -> f.name.equals("recipeLayout"));
        check(!RouterMixinPlugin.signatureMatches(withoutRecipe), "missing layout shadow disables pattern mixin");
        for (boolean curio : new boolean[] {false, true}) for (int slot : new int[] {-1, 0, 40, 4096}) {
            var value = new OpenTerminalRequestPayload(77, curio, slot, "ae2_wireless", 0);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                OpenTerminalRequestPayload.CODEC.encode(buffer, value);
                check(value.equals(OpenTerminalRequestPayload.CODEC.decode(buffer)), "request codec round trip " + curio + "/" + slot);
                check(buffer.readableBytes() == 0, "packet fully consumed");
            } finally { buffer.release(); }
        }
        System.out.println("PASS: compatibility guards and payload round trips");
    }
    private static ClassNode copy(ClassNode original) {
        var n = new ClassNode(); original.accept(n); return n;
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        System.out.println("PASS: " + label);
    }
}
