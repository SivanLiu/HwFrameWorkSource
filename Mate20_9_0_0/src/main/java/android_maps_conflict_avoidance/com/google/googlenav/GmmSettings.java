package android_maps_conflict_avoidance.com.google.googlenav;

import android_maps_conflict_avoidance.com.google.map.MapPoint;

public abstract class GmmSettings {
    private static final MapPoint FEATURE_TEST_DEFAULT_START = new MapPoint(40000000, -94000000);

    public static boolean isDebugBuild() {
        return false;
    }
}
