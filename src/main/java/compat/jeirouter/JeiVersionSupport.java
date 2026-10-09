package compat.jeirouter;

public final class JeiVersionSupport {
    public static final String LEGACY = "19.50.0.414";
    public static final String MODERN = "19.57.0.446";
    public static final String DESCRIPTION = LEGACY + " or " + MODERN;
    public static boolean supported(String version) { return LEGACY.equals(version) || MODERN.equals(version); }
    public static boolean legacy(String version) { return LEGACY.equals(version); }
    private JeiVersionSupport() {}
}
