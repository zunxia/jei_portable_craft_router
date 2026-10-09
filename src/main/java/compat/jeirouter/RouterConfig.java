package compat.jeirouter;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class RouterConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, AE2, UNIVERSAL, INTEGRATED, STICK, PATTERN, DEBUG;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PRIORITY;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Enable routing from the 2x2 inventory crafting grid.").define("enabled", true);
        AE2 = b.define("ae2WirelessCrafting", true);
        UNIVERSAL = b.define("ae2Universal", true);
        INTEGRATED = b.define("integratedPortable", true);
        STICK = b.define("craftingOnAStick", true);
        PATTERN = b.define("wirelessPatternEncoding", true);
        PRIORITY = b.comment("Adapter ids, highest priority first. Omitted ids are disabled.")
            .defineListAllowEmpty("priority", List.of("ae2_wireless", "ae2_universal", "integrated", "stick"),
                () -> "stick", x -> x instanceof String s && List.of("ae2_wireless", "ae2_universal", "integrated", "stick").contains(s));
        DEBUG = b.define("debugLogging", false);
        SPEC = b.build();
    }
    private RouterConfig() {}
}
