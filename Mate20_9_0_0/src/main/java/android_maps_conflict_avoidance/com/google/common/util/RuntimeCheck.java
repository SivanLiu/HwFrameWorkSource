package android_maps_conflict_avoidance.com.google.common.util;

public class RuntimeCheck {
    private static boolean isStartupComplete = false;
    private static boolean isTrackingStartup = false;

    public static boolean isTest() {
        return false;
    }
}
