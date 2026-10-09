package compat.jeirouter.mixin;

import java.util.List;
import java.util.Set;
import compat.jeirouter.JeiVersionSupport;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.slf4j.LoggerFactory;

public final class RouterMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        try {
            var mods = LoadingModList.get();
            String version = mods == null ? null : mods.getMods().stream().filter(m -> m.getModId().equals("jei"))
                .map(m -> m.getVersion().toString()).findFirst().orElse(null);
            if (!JeiVersionSupport.supported(version)) { warn("Supported JEI (" + JeiVersionSupport.DESCRIPTION + ") not present; routing mixin disabled"); return false; }
            boolean legacy = JeiVersionSupport.legacy(version);
            if (legacy != mixin.contains(".Legacy")) return false;
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(target);
            boolean compatible = signatureMatches(node, legacy);
            if (!compatible) warn("JEI controller signature changed; routing mixin disabled");
            return compatible;
        } catch (Exception | LinkageError e) {
            warn("Unable to inspect JEI controller; routing mixin disabled: " + e); return false;
        }
    }
    // 注入前核对所有 shadow 与入口，require=0 单独不能防止 shadow 缺失引起崩溃。
    public static boolean signatureMatches(ClassNode n) {
        return signatureMatches(n, false) || signatureMatches(n, true);
    }
    public static boolean signatureMatches(ClassNode n, boolean legacy) {
        if (n.name.equals("mezz/jei/gui/recipes/RecipeLayoutWithButtons")) return field(n, "buttons", "Ljava/util/List;") &&
            field(n, "recipeLayout", "Lmezz/jei/api/gui/IRecipeLayoutDrawable;") &&
            method(n, "getAbsoluteButtonArea", "(I)Lmezz/jei/common/util/ImmutableRect2i;") &&
            method(n, "create", "(Lmezz/jei/api/gui/IRecipeLayoutDrawable;Lmezz/jei/gui/bookmarks/RecipeBookmark;Lmezz/jei/gui/bookmarks/BookmarkList;Lmezz/jei/gui/recipes/RecipesGui;" +
                (legacy ? "" : "Lmezz/jei/common/transfer/RecipeTransferService;") + "Ljava/util/List;)Lmezz/jei/gui/recipes/IRecipeLayoutWithButtons;");
        return field(n, "recipeLayout", "Lmezz/jei/api/gui/IRecipeLayoutDrawable;") &&
            (legacy ? field(n, "recipesGui", "Lmezz/jei/gui/recipes/RecipesGui;") :
            (field(n, "recipeTransferService", "Lmezz/jei/common/transfer/RecipeTransferService;") &&
            field(n, "screenSupplier", "Ljava/util/function/Supplier;") &&
            field(n, "maxTransferSupplier", "Ljava/util/function/BooleanSupplier;"))) &&
            field(n, "recipeTransferError", "Lmezz/jei/api/recipe/transfer/IRecipeTransferError;") &&
            method(n, "updateState", "(Lmezz/jei/api/gui/buttons/IButtonState;)V") &&
            method(n, "onPress", "(Lmezz/jei/api/gui/inputs/IJeiUserInput;)Z") &&
            method(n, "getTooltips", "(Lmezz/jei/api/gui/builder/ITooltipBuilder;)V");
    }
    private static boolean field(ClassNode n, String name, String desc) { return n.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(desc)); }
    private static boolean method(ClassNode n, String name, String desc) { return n.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(desc)); }
    private static void warn(String message) { LoggerFactory.getLogger("jei_portable_craft_router").warn(message); }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        LoggerFactory.getLogger("jei_portable_craft_router").info("JEI portable routing mixin applied to {}", target);
    }
}
