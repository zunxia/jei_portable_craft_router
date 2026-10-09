package compat.routerprobe;

import compat.jeirouter.RouterConfig;
import compat.jeirouter.terminal.TerminalPriorityResolver;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.runtime.IJeiKeyMapping;
import mezz.jei.common.Internal;
import compat.jeirouter.apply.JeiTransferService;
import mezz.jei.gui.recipes.RecipeTransferButtonController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.api.config.Actionable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorageItem;

@Mod("router_probe")
public final class RouterProbe {
    @EventBusSubscriber(modid = "router_probe", value = Dist.CLIENT)
    public static final class Events {
        private static int stage, scenario = Integer.getInteger("router.probe.start", 0), tick, readyTick, deadline;
        private static final AtomicBoolean setupReady = new AtomicBoolean();
        private static volatile String failure;
        private static final AtomicBoolean verified = new AtomicBoolean();
        private static final String REMOTE = System.getProperty("router.probe.remote", "");
        private static final boolean DISABLED_CHECK = Boolean.getBoolean("router.probe.disabled");
        private static final boolean UNSUPPORTED_CHECK = Boolean.getBoolean("router.probe.unsupported");
        private static final boolean REMOTE_AE2 = Boolean.getBoolean("router.probe.remoteAe2");
        private static final boolean REMOTE_EMPTY = Boolean.getBoolean("router.probe.empty");
        private static final boolean REMOTE_PATTERN = Boolean.getBoolean("router.probe.pattern");
        private static compat.jeirouter.apply.RecipeSnapshot snapshot;
        private static double initialPower;
        private static volatile Runnable pendingGridSetup;
        private static String screenshot;
        private static boolean matrix() { return scenario >= 16 && scenario <= 27; }
        private static boolean universal() { return scenario == 7 || scenario == 15 || scenario == 49 || matrix() && scenario >= 22; }
        private static boolean curio() { return scenario == 10 || scenario == 15 || scenario == 49 || matrix() && (scenario - 16) % 6 >= 3; }
        private static int linkState() { return matrix() ? (scenario - 16) % 3 : 0; }
        private static boolean blocked() { return scenario >= 28 && scenario <= 33 || scenario == 40 || scenario == 41; }
        private static boolean pattern() { return scenario >= 42 && scenario <= 48 || scenario == 51 || scenario == 52; }
        @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
            if (screenshot == null || !(event.getScreen() instanceof mezz.jei.gui.recipes.RecipesGui gui)) return;
            try {
                var button = buttons(gui).get(scenario == 51 ? 1 : 0);
                gui.render(event.getGuiGraphics(), button.getX() + 7, button.getY() + 7, event.getPartialTick());
                event.getGuiGraphics().flush();
                var output = Minecraft.getInstance().gameDirectory.toPath().resolve(screenshot);
                try (var image = net.minecraft.client.Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(output); }
                pass("visual screenshot saved: " + output); screenshot = null;
            } catch (Throwable e) { failure = e.toString(); }
        }
        private static Object field(Object object, String name) throws Exception {
            var value = object.getClass().getDeclaredField(name); value.setAccessible(true); return value.get(object);
        }
        @SuppressWarnings("unchecked") private static List<mezz.jei.gui.elements.IconButton> buttons(mezz.jei.gui.recipes.RecipesGui gui) throws Exception {
            var layouts = (List<?>) field(field(gui, "layouts"), "recipeLayoutsWithButtons");
            return (List<mezz.jei.gui.elements.IconButton>) field(layouts.getFirst(), "buttons");
        }
        private static void pass(String s) { LoggerFactory.getLogger("router_probe").info("PROBE PASS: {}", s); }
        private static void check(boolean b, String s) { if (!b) throw new AssertionError(s); pass(s); }
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance();
            tick++;
            try {
                if (failure != null) throw new AssertionError(failure);
                if (stage == 0 && mc.screen instanceof TitleScreen) {
                    if (Boolean.getBoolean("router.probe.bootstrapOnly")) {
                        check(RouterConfig.SPEC.isLoaded(), "COMMON config loaded before title screen");
                        pass("FULL PACK TITLE SCREEN REACHED");
                        stage = 9; mc.stop(); return;
                    }
                    stage = 1; deadline = tick + 5000;
                    if (!REMOTE.isEmpty()) {
                        scenario = REMOTE_PATTERN ? 42 : 2;
                        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen, mc,
                            net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(REMOTE),
                            new net.minecraft.client.multiplayer.ServerData("Router probe", REMOTE, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);
                        pass("connecting separate client to dedicated server");
                        return;
                    }
                    var settings = new LevelSettings("Router isolated probe", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("RouterProbe-" + System.currentTimeMillis(), settings,
                        new WorldOptions(1234L, false, false),
                        r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                    pass("isolated client booted and world creation started");
                }
                if (stage > 0 && stage < 9 && tick > deadline) throw new AssertionError("scenario timeout: " + scenario + "/" + stage);
                if (stage == 1 && mc.player != null && mc.level != null && Internal.getOptionalJeiRuntime().isPresent()) {
                    if (!REMOTE.isEmpty()) { stage = 3; readyTick = tick + 40; deadline = tick + 400; return; }
                    startSetup(); stage = 2;
                }
                if (stage == 2 && !setupReady.get() && pendingGridSetup != null) mc.getSingleplayerServer().execute(() -> {
                    var task = pendingGridSetup;
                    if (task != null) try { task.run(); } catch (Throwable e) { failure = e.toString(); }
                });
                if (stage == 2 && setupReady.get()) { stage = 3; readyTick = tick + 30; }
                if (stage == 3 && tick >= readyTick) runScenario();
                if (stage == 8 && tick >= readyTick) verifyPattern();
                if (stage == 11 && tick >= readyTick) {
                    var gui = (mezz.jei.gui.recipes.RecipesGui) mc.screen;
                    var buttons = buttons(gui);
                    check(field(buttons.get(1), "controller") instanceof compat.jeirouter.client.PatternButtonController, "pattern button is second button directly below transfer");
                    check(buttons.get(1).isVisible() && buttons.get(1).getX() == buttons.get(0).getX() && buttons.get(1).getY() > buttons.get(0).getY(), "native JEI layout places pattern button below transfer without overlap");
                    var icon = field(field(buttons.get(1), "button"), "icon");
                    var ingredient = (mezz.jei.api.ingredients.ITypedIngredient<?>) field(field(icon, "item"), "typedIngredient");
                    check(ingredient.getIngredient() instanceof ItemStack stack && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("ae2:blank_pattern"), "pattern button overlays exact AE blank pattern icon");
                    screenshot = "p3-pattern-button.png"; stage = 12;
                }
                if (stage == 12 && screenshot == null) {
                    var gui = (mezz.jei.gui.recipes.RecipesGui) mc.screen;
                    var button = buttons(gui).get(1);
                    check(gui.mouseClicked(button.getX() + 7, button.getY() + 7, 0), "real JEI mouse click activates pattern button");
                    check(gui.mouseReleased(button.getX() + 7, button.getY() + 7, 0), "real JEI mouse release executes pattern action");
                    readyTick = tick + 60; stage = 8;
                }
                if (stage == 10 && tick >= readyTick) {
                    check(mc.player.containerMenu instanceof appeng.menu.me.crafting.CraftConfirmMenu, "saved Ctrl click reaches native AE autocrafting confirmation");
                    mc.getSingleplayerServer().execute(() -> {
                        try {
                            var sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                            check(sp.containerMenu instanceof appeng.menu.me.crafting.CraftConfirmMenu, "server handles native craft-missing packet and opens confirmation");
                            verified.set(true);
                        } catch (Throwable e) { failure = e.toString(); }
                    });
                    stage = 7;
                }
                if (stage == 4 && mc.player == null) throw new AssertionError("client disconnected before terminal was established");
                if (stage == 4 && mc.player.containerMenu != mc.player.inventoryMenu && tick >= readyTick) {
                    if (!REMOTE.isEmpty()) {
                        check(REMOTE_PATTERN ? mc.player.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu : REMOTE_AE2 ? mc.player.containerMenu instanceof CraftingTermMenu : mc.player.containerMenu instanceof CraftingMenu, "dedicated server opened expected portable terminal for " + mc.player.getName().getString());
                        if (REMOTE_PATTERN) {
                            check(java.util.Arrays.stream(((appeng.menu.me.items.PatternEncodingTermMenu) mc.player.containerMenu).getCraftingGridSlots()).filter(s -> s.getItem().is(Items.OAK_PLANKS)).count() == 8, "dedicated client receives pattern ghost slots");
                        } else if (REMOTE_EMPTY) {
                            check(!compat.jeirouter.apply.MaterialGate.check(mc.player.containerMenu, snapshot).enough(), "dedicated client material gate blocks missing ingredients");
                            check(mc.player.containerMenu.slots.stream().noneMatch(s -> s.container != mc.player.getInventory() && s.getItem().is(Items.CHEST)), "dedicated client has no manufactured output");
                        } else check(mc.player.containerMenu.slots.stream().anyMatch(s -> s.container != mc.player.getInventory() && s.getItem().is(Items.CHEST)), "dedicated server synced chest output to " + mc.player.getName().getString());
                        pass("REMOTE CLIENT COMPLETE"); stage = 9; mc.stop(); return;
                    }
                    check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> || scenario == 34 && mc.screen instanceof mezz.jei.gui.recipes.RecipesGui, "terminal screen established scenario " + scenario);
                    var gate = compat.jeirouter.apply.MaterialGate.check(mc.player.containerMenu, snapshot);
                    if (blocked()) check(!gate.enough(), "material gate blocks missing ingredients scenario " + scenario);
                    if (scenario == 34) {
                        var terminal = (CraftingTermMenu) mc.player.containerMenu;
                        var missing = terminal.findMissingIngredients(compat.jeirouter.apply.RouterJeiPlugin.transferHelper().getGuiSlotIndexToIngredientMap(snapshot.craftingRecipe()));
                        check(missing.anyCraftable() && !missing.anyMissing(), "zero-stock craftable ingredients are accepted");
                        check(gate.verdict() == compat.jeirouter.apply.MaterialGate.Verdict.CRAFTABLE, "craftable ingredients require explicit Ctrl confirmation");
                        check(mc.screen instanceof mezz.jei.gui.recipes.RecipesGui, "craftable route reopens recipe over actual terminal");
                        var actualScreen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) Internal.getJeiRuntime().getRecipesGui().getParentScreen().orElseThrow();
                        var control = transferController(snapshot.layout(), actualScreen);
                        var state = new ButtonState(); control.initState(state);
                        var collectType = Class.forName("compat.jeirouter.client.ErrorTooltipCollector");
                        var collectCtor = collectType.getDeclaredConstructor(); collectCtor.setAccessible(true);
                        var tooltip = (mezz.jei.api.gui.builder.ITooltipBuilder) collectCtor.newInstance();
                        control.getTooltips(tooltip);
                        check(tooltip.getLines().stream().flatMap(l -> l.left().stream()).anyMatch(line -> line instanceof net.minecraft.network.chat.Component c &&
                            c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("router.ctrl_craft") &&
                            c.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.BLUE))), "exact Ctrl tooltip is blue");
                        var nativeError = new JeiTransferService(Internal.getJeiRuntime().getRecipeTransferManager()).getTransferRecipeError(actualScreen, snapshot.layout(), mc.player).orElseThrow();
                        check(nativeError.getType() == mezz.jei.api.recipe.transfer.IRecipeTransferError.Type.COSMETIC && nativeError.getButtonHighlightColor() == appeng.integration.modules.itemlists.TransferHelper.BLUE_PLUS_BUTTON_COLOR,
                            "native JEI error renderer supplies blue button and craftable slots");
                        screenshot = "p3-blue-tooltip.png";
                    }
                    stage = 5;
                    mc.getSingleplayerServer().execute(() -> {
                        try {
                            var sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                            check(sp.containerMenu instanceof CraftingMenu || sp.containerMenu instanceof CraftingTermMenu || sp.containerMenu instanceof ContainerTerminalStorageItem,
                                "server opened actual portable crafting menu scenario " + scenario);
                            int filled = 0;
                            for (var slot : sp.containerMenu.slots) if (slot.container != sp.getInventory() && slot.getItem().is(Items.OAK_PLANKS)) filled++;
                            if (blocked() || scenario == 34) {
                                check(filled == 0, "precheck leaves crafting grid untouched scenario " + scenario);
                                check(sp.containerMenu.slots.stream().noneMatch(s -> s.container != sp.getInventory() && s.getItem().is(Items.CHEST)), "no output manufactured scenario " + scenario);
                                verified.set(true); return;
                            }
                            check(filled == 8, "server crafting matrix has eight planks scenario " + scenario);
                            if (scenario == 13) check(sp.containerMenu.getSlot(1).getItem().getCount() == 8, "saved max-transfer fills eight complete sets");
                            check(sp.containerMenu.slots.stream().anyMatch(s -> s.container != sp.getInventory() && s.getItem().is(Items.CHEST)), "server output slot contains chest scenario " + scenario);
                            verified.set(true);
                        } catch (Throwable e) { failure = e.toString(); }
                    });
                }
                if (stage == 5 && verified.get() && screenshot == null) {
                    if (scenario == 49) {
                        verified.set(false);
                        compat.jeirouter.client.ClientRouter.beginPattern(snapshot.layout(), new JeiTransferService(Internal.getJeiRuntime().getRecipeTransferManager()));
                        readyTick = tick + 60; stage = 8; return;
                    }
                    if (scenario == 15) {
                        verified.set(false);
                        var output = mc.player.containerMenu.slots.stream().filter(s -> s.container != mc.player.getInventory() && s.getItem().is(Items.CHEST)).findFirst().orElseThrow();
                        var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.screen;
                        // AE2 的成品槽使用其屏幕自带点击协议，不能发送原版槽位点击包。
                        screen.mouseClicked(screen.getGuiLeft() + output.x + 8, screen.getGuiTop() + output.y + 8, 0);
                        screen.mouseReleased(screen.getGuiLeft() + output.x + 8, screen.getGuiTop() + output.y + 8, 0);
                        readyTick = tick + 30; stage = 6;
                    } else next();
                }
                if (stage == 6 && tick >= readyTick) {
                    stage = 7;
                    mc.getSingleplayerServer().execute(() -> {
                        try {
                            var sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                            check(sp.containerMenu.getCarried().is(Items.CHEST), "player can take crafted output from Curios universal terminal");
                            var terminal = CuriosApi.getCuriosInventory(sp).orElseThrow().getEquippedCurios().getStackInSlot(0);
                            check(((WirelessTerminalItem) terminal.getItem()).getAECurrentPower(terminal) < initialPower, "terminal drains power from actual equipped item");
                            sp.closeContainer(); verified.set(true);
                        } catch (Throwable e) { failure = e.toString(); }
                    });
                }
                if (stage == 7 && verified.get()) next();
            } catch (Throwable e) {
                stage = 9;
                LoggerFactory.getLogger("router_probe").error("PROBE FAIL", e);
                mc.stop();
            }
        }
        private static void startSetup() {
            var mc = Minecraft.getInstance();
            // Close the previous recipe normally so JEI clears its parent and recipe history.
            if (mc.screen instanceof mezz.jei.gui.recipes.RecipesGui gui) gui.onClose();
            setupReady.set(false); verified.set(false); pendingGridSetup = null; deadline = tick + 600;
            RouterConfig.ENABLED.set(true);
            mc.getSingleplayerServer().execute(() -> {
                try {
                    var sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    sp.closeContainer(); sp.getInventory().clearContent();
                    CuriosApi.getCuriosInventory(sp).ifPresent(h -> {
                        var inv = h.getEquippedCurios();
                        for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
                    });
                    sp.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 64));
                    var stick = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("crafting_on_a_stick:crafting_table")));
                    if (scenario == 2 || scenario == 3 || scenario == 5 || scenario == 8 || scenario == 9 || scenario == 13 || matrix() && linkState() != 0 || scenario == 30 || scenario == 31 || scenario == 35 || scenario == 37) sp.getInventory().setItem(0, stick);
                    if (scenario == 37) sp.getInventory().setItem(2, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2wtlib:wireless_universal_terminal"))));
                    if (scenario == 3) {
                        sp.getInventory().setItem(2, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:wireless_crafting_terminal"))));
                        sp.getInventory().setItem(3, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("integratedterminals:terminal_storage_portable"))));
                    }
                    if (scenario == 4) {
                        var handler = CuriosApi.getCuriosInventory(sp).orElseThrow();
                        check(handler.getEquippedCurios().getSlots() > 0, "test curio slots available");
                        handler.getEquippedCurios().setStackInSlot(0, stick);
                    }
                    if ((scenario >= 6 && scenario <= 10) || scenario == 14 || scenario == 15 || matrix() || scenario == 28 || scenario == 29 || scenario == 34 || scenario == 36 || pattern() || scenario == 49 || scenario == 50) {
                        var energyPos = new BlockPos(4, -59, 4);
                        var accessPos = energyPos.above();
                        sp.teleportTo(0.5, -58, 0.5);
                        sp.serverLevel().setBlockAndUpdate(energyPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")).defaultBlockState());
                        sp.serverLevel().setBlockAndUpdate(accessPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:wireless_access_point")).defaultBlockState());
                        appeng.api.orientation.BlockOrientation.UP_NORTH.setOn(sp.serverLevel(), accessPos);
                        var terminal = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(scenario == 7 ? "ae2wtlib:wireless_universal_terminal" : "ae2:wireless_crafting_terminal")));
                        if (universal()) terminal = de.mari_023.ae2wtlib.wut.WTDefinitions.CRAFTING.universalTerminal().copy();
                        if (pattern()) {
                            terminal = scenario == 44 || scenario == 45 || scenario == 47
                                ? de.mari_023.ae2wtlib.wut.WTDefinitions.CRAFTING.universalTerminal().copy()
                                : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2wtlib:wireless_pattern_encoding_terminal")));
                            if (scenario == 44 || scenario == 45) terminal.set(de.mari_023.ae2wtlib.wut.WTDefinitions.PATTERN_ENCODING.componentType(), com.mojang.datafixers.util.Unit.INSTANCE);
                            sp.getInventory().setItem(1, ItemStack.EMPTY);
                        }
                        if (scenario == 15 || scenario == 49) {
                            terminal.set(de.mari_023.ae2wtlib.wut.WTDefinitions.PATTERN_ENCODING.componentType(), com.mojang.datafixers.util.Unit.INSTANCE);
                            terminal.set(de.mari_023.ae2wtlib.api.AE2wtlibComponents.CURRENT_TERMINAL, de.mari_023.ae2wtlib.wut.WTDefinitions.PATTERN_ENCODING);
                        }
                        var item = (WirelessTerminalItem) terminal.getItem();
                        if (scenario != 8 && linkState() != 2) item.injectAEPower(terminal, item.getAEMaxPower(terminal), Actionable.MODULATE);
                        initialPower = item.getAECurrentPower(terminal);
                        if (linkState() != 1 && scenario != 52) WirelessTerminalItem.LINKABLE_HANDLER.link(terminal, GlobalPos.of(sp.level().dimension(), accessPos));
                        if (scenario == 9) sp.teleportTo(120.5, -58, 120.5);
                        if (curio() || scenario == 43 || scenario == 45) CuriosApi.getCuriosInventory(sp).orElseThrow().getEquippedCurios().setStackInSlot(0, terminal);
                        else sp.getInventory().setItem(2, terminal);
                        if (scenario == 52) {
                            var universal = de.mari_023.ae2wtlib.wut.WTDefinitions.PATTERN_ENCODING.universalTerminal().copy();
                            var universalItem = (WirelessTerminalItem) universal.getItem();
                            universalItem.injectAEPower(universal, universalItem.getAEMaxPower(universal), Actionable.MODULATE);
                            WirelessTerminalItem.LINKABLE_HANDLER.link(universal, GlobalPos.of(sp.level().dimension(), accessPos));
                            CuriosApi.getCuriosInventory(sp).orElseThrow().getEquippedCurios().setStackInSlot(0, universal);
                        }
                        if (scenario == 36) {
                            var universal = de.mari_023.ae2wtlib.wut.WTDefinitions.CRAFTING.universalTerminal().copy();
                            var universalItem = (WirelessTerminalItem) universal.getItem();
                            universalItem.injectAEPower(universal, universalItem.getAEMaxPower(universal), Actionable.MODULATE);
                            WirelessTerminalItem.LINKABLE_HANDLER.link(universal, GlobalPos.of(sp.level().dimension(), accessPos));
                            CuriosApi.getCuriosInventory(sp).orElseThrow().getEquippedCurios().setStackInSlot(0, universal);
                        }
                        if (scenario == 14 || scenario == 28 || scenario == 29 || scenario == 34 || scenario == 50) {
                            var chestPos = energyPos.east();
                            sp.serverLevel().setBlockAndUpdate(chestPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:chest")).defaultBlockState());
                            var chest = (appeng.blockentity.storage.MEChestBlockEntity) sp.serverLevel().getBlockEntity(chestPos);
                            chest.getInternalInventory().setItemDirect(1, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:item_storage_cell_1k"))));
                            int amount = scenario == 14 ? 64 : scenario == 29 ? 7 : 0;
                            long inserted = chest.getCellInventory(0).insert(appeng.api.stacks.AEItemKey.of(Items.OAK_PLANKS), amount, Actionable.MODULATE, appeng.api.networking.security.IActionSource.ofPlayer(sp));
                            check(inserted == amount, amount + " planks stored in AE2 cell");
                            sp.getInventory().setItem(1, ItemStack.EMPTY);
                            if (scenario == 34 || scenario == 50) {
                                var patternStack = appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                                    List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG), 1)),
                                    List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(Items.OAK_PLANKS), 4)));
                                var pattern = appeng.api.crafting.PatternDetailsHelper.decodePattern(patternStack, sp.serverLevel());
                                var provider = new appeng.api.networking.crafting.ICraftingProvider() {
                                    public List<appeng.api.crafting.IPatternDetails> getAvailablePatterns() { return List.of(pattern); }
                                    public boolean pushPattern(appeng.api.crafting.IPatternDetails p, appeng.api.stacks.KeyCounter[] inputs) { return false; }
                                    public boolean isBusy() { return false; }
                                };
                                var linkedTerminal = terminal;
                                pendingGridSetup = () -> {
                                    var grid = item.getLinkedGrid(linkedTerminal, sp.level(), m -> {});
                                    if (grid == null) return;
                                    grid.getCraftingService().addGlobalCraftingProvider(provider);
                                    pendingGridSetup = null; setupReady.set(true);
                                };
                            }
                        }
                    }
                    if (scenario == 11 || scenario == 12 || scenario == 32 || scenario == 33 || scenario >= 39 && scenario <= 41) {
                        sp.teleportTo(0.5, -58, 0.5);
                        var cablePos = new BlockPos(8, -58, 8);
                        sp.serverLevel().setBlockAndUpdate(cablePos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("integrateddynamics:cable")).defaultBlockState());
                        var partPos = org.cyclops.integrateddynamics.api.part.PartPos.of(sp.serverLevel(), cablePos, net.minecraft.core.Direction.NORTH);
                        org.cyclops.integrateddynamics.core.helper.NetworkHelpers.initNetwork(sp.serverLevel(), cablePos, net.minecraft.core.Direction.NORTH);
                        org.cyclops.integrateddynamics.part.PartTypeConnectorOmniDirectional.LOADED_GROUPS.addPosition(77, partPos, false);
                        if (scenario >= 39 && scenario <= 41) {
                            var chestPos = cablePos.east();
                            sp.serverLevel().setBlockAndUpdate(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) sp.serverLevel().getBlockEntity(chestPos);
                            int count = scenario == 39 ? 64 : scenario == 40 ? 7 : 0;
                            chest.clearContent();
                            if (count > 0) chest.setItem(0, new ItemStack(Items.OAK_PLANKS, count));
                            var network = org.cyclops.integrateddynamics.core.helper.NetworkHelpers.getNetwork(partPos).orElseThrow();
                            var itemNetwork = network.getCapability(org.cyclops.integratedtunnels.Capabilities.ItemNetwork.NETWORK).orElseThrow();
                            var chestPart = org.cyclops.integrateddynamics.api.part.PartPos.of(sp.serverLevel(), chestPos, net.minecraft.core.Direction.NORTH);
                            itemNetwork.addPosition(chestPart, 0, 0);
                            itemNetwork.scheduleObservationForced(0, chestPart);
                            itemNetwork.runObserverSync();
                            check(itemNetwork.getPositionedStorage(chestPart) != null, "real chest registered as Integrated network storage");
                            sp.getInventory().setItem(1, ItemStack.EMPTY);
                        }
                        var terminal = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("integratedterminals:terminal_storage_portable")));
                        org.cyclops.integratedterminals.item.ItemTerminalStoragePortable.setGroupId(terminal, 77);
                        if (scenario == 12) CuriosApi.getCuriosInventory(sp).orElseThrow().getEquippedCurios().setStackInSlot(0, terminal);
                        else sp.getInventory().setItem(2, terminal);
                    }
                    if (scenario == 30 || scenario == 32) sp.getInventory().setItem(1, ItemStack.EMPTY);
                    if (scenario == 31 || scenario == 33) sp.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 7));
                    sp.inventoryMenu.broadcastChanges(); if (pendingGridSetup == null) setupReady.set(true);
                } catch (Throwable e) { failure = e.toString(); }
            });
        }
        @SuppressWarnings("unchecked")
        private static void runScenario() {
            var mc = Minecraft.getInstance();
            mc.setScreen(new InventoryScreen(mc.player));
            var runtime = Internal.getJeiRuntime();
            var category = runtime.getRecipeManager().createRecipeCategoryLookup().limitTypes(List.of(RecipeTypes.CRAFTING)).get().findFirst().orElseThrow();
            var recipe = (RecipeHolder<CraftingRecipe>) mc.level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(scenario == 1 ? "crafting_table" : "chest")).orElseThrow();
            var layout = runtime.getRecipeManager().createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory<RecipeHolder<CraftingRecipe>>) category,
                recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
            snapshot = compat.jeirouter.apply.RecipeSnapshot.capture(layout, scenario == 13);
            if (scenario == 38) {
                var smeltingCategory = runtime.getRecipeManager().createRecipeCategoryLookup().limitTypes(List.of(RecipeTypes.SMELTING)).get().findFirst().orElseThrow();
                var smeltingRecipe = mc.level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("iron_ingot_from_smelting_raw_iron")).orElseThrow();
                var smeltingLayout = runtime.getRecipeManager().createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory) smeltingCategory,
                    smeltingRecipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
                var nonCrafting = compat.jeirouter.apply.RecipeSnapshot.capture((IRecipeLayoutDrawable<?>) smeltingLayout, false);
                var menu = new CraftingMenu(-1, mc.player.getInventory());
                var unknown = compat.jeirouter.apply.MaterialGate.check(menu, nonCrafting);
                check(unknown.verdict() == compat.jeirouter.apply.MaterialGate.Verdict.UNKNOWN, "non-crafting input produces UNKNOWN material verdict");
                var result = compat.jeirouter.apply.RecipeApplier.apply(nonCrafting, new JeiTransferService(runtime.getRecipeTransferManager()),
                    new net.minecraft.client.gui.screens.inventory.CraftingScreen(menu, mc.player.getInventory(), net.minecraft.network.chat.Component.literal("probe")));
                check(!result.success(), "UNKNOWN verdict blocks automatic apply");
                next(); return;
            }
            if (scenario == 35) {
                var unknownMenu = new CraftingMenu(-1, mc.player.getInventory()) {
                    public net.minecraft.world.inventory.MenuType<?> getType() { return net.minecraft.world.inventory.MenuType.HOPPER; }
                };
                var gate = compat.jeirouter.apply.MaterialGate.check(unknownMenu, snapshot);
                check(!gate.enough() && gate.reason().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("router.jei_cannot_apply"), "missing JEI handler is distinguished from missing materials");
                next(); return;
            }
            var service = new JeiTransferService(runtime.getRecipeTransferManager());
            if (pattern()) {
                IRecipeLayoutDrawable<?> patternLayout = layout;
                if (scenario == 46) {
                    var smeltingCategory = runtime.getRecipeManager().createRecipeCategoryLookup().limitTypes(List.of(RecipeTypes.SMELTING)).get().findFirst().orElseThrow();
                    var smeltingRecipe = mc.level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("iron_ingot_from_smelting_raw_iron")).orElseThrow();
                    patternLayout = (IRecipeLayoutDrawable<?>) runtime.getRecipeManager().createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory) smeltingCategory,
                        smeltingRecipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
                }
                snapshot = compat.jeirouter.apply.RecipeSnapshot.capture(patternLayout, false);
                var gui = (mezz.jei.gui.recipes.RecipesGui) runtime.getRecipesGui();
                // Refresh JEI's parent screen for each scenario, just as opening a recipe from inventory does.
                ((mezz.jei.api.runtime.IRecipesGui) gui).showRecipes((mezz.jei.api.recipe.category.IRecipeCategory) patternLayout.getRecipeCategory(), List.of(patternLayout.getRecipe()), List.of());
                if (scenario == 51) {
                    stage = 11; readyTick = tick + 20; return;
                }
                var controller = new compat.jeirouter.client.PatternButtonController(patternLayout, gui, service);
                var state = new ButtonState();
                if (scenario == 48) RouterConfig.PATTERN.set(false);
                controller.initState(state);
                if (scenario == 47 || scenario == 48) {
                    check(!state.visible && !state.active, "pattern button hidden without encoding module or when disabled scenario " + scenario);
                    RouterConfig.PATTERN.set(true); next(); return;
                }
                check(state.visible && state.active, "pattern button visible and active scenario " + scenario);
                var candidates = TerminalPriorityResolver.findPatterns(mc.player);
                check(!candidates.isEmpty() && candidates.getFirst().reference().curio() == (scenario == 43 || scenario == 45 || REMOTE_PATTERN), "pattern resolver locates correct inventory or Curios slot");
                check(controller.onPress(new Input(true)) && mc.player.containerMenu == mc.player.inventoryMenu, "simulated pattern click sends no open request");
                check(controller.onPress(new Input(false)), "actual pattern button accepts click");
                readyTick = tick + 60; stage = REMOTE_PATTERN ? 4 : 8; return;
            }
            var inventoryScreen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.screen;
            runtime.getRecipesGui().showRecipes((mezz.jei.api.recipe.category.IRecipeCategory) category, List.of(recipe), List.of());
            var controller = transferController(layout, inventoryScreen);
            var state = new ButtonState();
            if (scenario == 5) RouterConfig.ENABLED.set(false);
            controller.initState(state);
            if (DISABLED_CHECK || UNSUPPORTED_CHECK) {
                check(!state.active, UNSUPPORTED_CHECK ? "server without JEI keeps oversized button disabled" : "incompatible JEI version boots and keeps oversized button disabled");
                if (UNSUPPORTED_CHECK) {
                    compat.jeirouter.client.ClientRouter.tick();
                    check(!compat.jeirouter.client.ClientRouter.serverSupported(), "server JEI handshake prevents all routing packets");
                }
                pass("DISABLED VERSION CHECK COMPLETE"); stage = 9; mc.stop(); return;
            }
            if (scenario == 0 || scenario == 5) {
                check(!state.active, scenario == 0 ? "no terminal preserves disabled oversized button" : "global toggle preserves original disabled button");
                next(); return;
            }
            if (scenario == 1) { check(state.active, "normal 2x2 recipe remains transferable"); next(); return; }
            check(state.active, "oversized button enabled scenario " + scenario);
            if (scenario == 36) {
                var chosen = TerminalPriorityResolver.find(mc.player).getFirst();
                check(chosen.adapter().id().equals("ae2_wireless") && !chosen.reference().curio(), "inventory wireless terminal wins over equipped universal terminal");
            }
            if (scenario == 37) check(TerminalPriorityResolver.find(mc.player).stream().noneMatch(c -> c.adapter().id().equals("ae2_universal")), "universal terminal without crafting module is filtered before requesting open");
            if (scenario == 30 || scenario == 31) {
                var preview = compat.jeirouter.apply.MaterialGate.preview(snapshot, "stick");
                check(preview.verdict() == compat.jeirouter.apply.MaterialGate.Verdict.NOT_ENOUGH, "workbench missing materials known before click");
            }
            if (scenario == 3) check(TerminalPriorityResolver.find(mc.player).getFirst().adapter().id().equals("ae2_wireless"), "AE2 wins priority before fallback");
            if (scenario == 4) check(TerminalPriorityResolver.find(mc.player).getFirst().reference().curio(), "workbench detected in Curios");
            controller.onPress(new Input(true));
            check(mc.player.containerMenu == mc.player.inventoryMenu, "simulated click does not open menu");
            if (scenario == 50) { controller.onPress(new Input(false, org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL)); readyTick = tick + 100; stage = 10; return; }
            if (scenario == 13) compat.jeirouter.client.ClientRouter.begin(layout, service, true);
            else controller.onPress(new Input(false));
            readyTick = tick + (REMOTE_EMPTY ? 100 : 60); stage = 4;
        }
        private static void verifyPattern() throws Exception {
            var mc = Minecraft.getInstance();
            check(mc.player.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu, "wireless pattern menu established scenario " + scenario);
            check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>, "wireless pattern screen established");
            var runtime = Internal.getJeiRuntime();
            var category = runtime.getRecipeManager().createRecipeCategoryLookup().limitTypes(List.of(RecipeTypes.CRAFTING)).get().findFirst().orElseThrow();
            var recipe = (RecipeHolder<CraftingRecipe>) mc.level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("chest")).orElseThrow();
            var layout = runtime.getRecipeManager().createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory<RecipeHolder<CraftingRecipe>>) category,
                recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
            ((mezz.jei.api.runtime.IRecipesGui) runtime.getRecipesGui()).showRecipes((mezz.jei.api.recipe.category.IRecipeCategory) category, List.of(recipe), List.of());
            var nativeButton = new compat.jeirouter.client.PatternButtonController(layout, (mezz.jei.gui.recipes.RecipesGui) runtime.getRecipesGui(),
                new JeiTransferService(runtime.getRecipeTransferManager()));
            var nativeState = new ButtonState(); nativeButton.initState(nativeState);
            check(!nativeState.visible && !nativeState.active, "duplicate pattern button is hidden over native AE encoding screen");
            check(!nativeButton.onPress(new Input(true)), "hidden pattern route rejects simulated input on native encoding screen");
            var nativeButtons = buttons((mezz.jei.gui.recipes.RecipesGui) runtime.getRecipesGui());
            check(nativeButtons.getFirst().isVisible() && field(nativeButtons.getFirst(), "controller") instanceof RecipeTransferButtonController,
                "native JEI encoding transfer button remains visible");
            stage = 7;
            mc.getSingleplayerServer().execute(() -> {
                try {
                    var sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    check(sp.containerMenu instanceof appeng.menu.me.items.PatternEncodingTermMenu, "server opens actual encoding menu");
                    var menu = (appeng.menu.me.items.PatternEncodingTermMenu) sp.containerMenu;
                    if (scenario == 46) {
                        check(menu.getMode() == appeng.parts.encoding.EncodingMode.PROCESSING, "noncrafting recipe selects processing pattern mode");
                        check(java.util.Arrays.stream(menu.getProcessingInputSlots()).anyMatch(s -> s.getItem().is(Items.RAW_IRON)) &&
                            java.util.Arrays.stream(menu.getProcessingOutputSlots()).anyMatch(s -> s.getItem().is(Items.IRON_INGOT)), "processing pattern has real recipe inputs and output");
                    } else {
                        check(menu.getMode() == appeng.parts.encoding.EncodingMode.CRAFTING, "crafting pattern mode selected");
                        check(java.util.Arrays.stream(menu.getCraftingGridSlots()).filter(s -> s.getItem().is(Items.OAK_PLANKS)).count() == 8, "eight recipe ghost slots written without owning planks");
                    }
                    check(sp.getInventory().countItem(Items.OAK_PLANKS) == (scenario == 49 ? 56 : 0) && sp.getInventory().countItem(Items.CHEST) == 0, "pattern route neither consumes nor manufactures recipe items");
                    verified.set(true);
                } catch (Throwable e) { failure = e.toString(); }
            });
        }
        private static RecipeTransferButtonController transferController(IRecipeLayoutDrawable<?> layout,
                net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
            try {
                var gui = Internal.getJeiRuntime().getRecipesGui();
                var constructors = RecipeTransferButtonController.class.getDeclaredConstructors();
                var legacy = java.util.Arrays.stream(constructors).filter(c -> c.getParameterCount() == 2).findFirst();
                if (legacy.isPresent()) return (RecipeTransferButtonController) legacy.get().newInstance(layout, gui);
                var modernType = Class.forName("mezz.jei.common.transfer.RecipeTransferService");
                var nativeService = modernType.getConstructor(mezz.jei.api.recipe.transfer.IRecipeTransferManager.class)
                    .newInstance(Internal.getJeiRuntime().getRecipeTransferManager());
                var constructor = java.util.Arrays.stream(constructors).filter(c -> c.getParameterCount() == 5).findFirst().orElseThrow();
                constructor.setAccessible(true);
                return (RecipeTransferButtonController) constructor.newInstance(layout, nativeService,
                    (java.util.function.Supplier<net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>>) () -> screen,
                    (java.util.function.BooleanSupplier) () -> false, (Runnable) () -> {});
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
        private static void next() {
            if (++scenario > Integer.getInteger("router.probe.end", 15)) {
                pass("ALL SCENARIOS COMPLETE"); stage = 9; Minecraft.getInstance().stop();
            } else { startSetup(); stage = 2; }
        }
    }
    private static final class ButtonState implements IButtonState {
        boolean active, visible;
        public void setActive(boolean b) { active = b; }
        public void setVisible(boolean b) { visible = b; }
        public void setIcon(IDrawable icon) {}
        public void setForcePressed(boolean b) {}
    }
    private record Input(boolean isSimulate, int getModifiers) implements IJeiUserInput {
        Input(boolean simulate) { this(simulate, 0); }
        public InputConstants.Key getKey() { return InputConstants.Type.MOUSE.getOrCreate(0); }
        public boolean is(KeyMapping mapping) { return false; }
        public boolean is(IJeiKeyMapping mapping) { return false; }
    }
}
