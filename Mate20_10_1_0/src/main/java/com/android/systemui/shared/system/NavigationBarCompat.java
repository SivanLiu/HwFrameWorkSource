package com.android.systemui.shared.system;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NavigationBarCompat extends QuickStepContract {
    public static final int FLAG_DISABLE_QUICK_SCRUB = 2;
    public static final int FLAG_DISABLE_SWIPE_UP = 1;
    public static final int FLAG_SHOW_OVERVIEW_BUTTON = 4;
    public static final int HIT_TARGET_BACK = 1;
    public static final int HIT_TARGET_DEAD_ZONE = 5;
    public static final int HIT_TARGET_HOME = 2;
    public static final int HIT_TARGET_NONE = 0;
    public static final int HIT_TARGET_OVERVIEW = 3;
    public static final int HIT_TARGET_ROTATION = 4;

    @Retention(RetentionPolicy.SOURCE)
    public @interface HitTarget {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface InteractionType {
    }
}
