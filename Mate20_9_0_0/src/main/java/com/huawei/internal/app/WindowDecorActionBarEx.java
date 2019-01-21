package com.huawei.internal.app;

import android.app.ActionBar;
import com.android.internal.app.WindowDecorActionBar;

public class WindowDecorActionBarEx {
    public static void setShowHideAnimationEnabled(ActionBar actionBar, boolean enabled) {
        if (actionBar instanceof WindowDecorActionBar) {
            ((WindowDecorActionBar) actionBar).setShowHideAnimationEnabled(enabled);
        }
    }

    public static void setAnimationEnable(ActionBar actionBar, boolean enabled) {
        if (actionBar instanceof WindowDecorActionBar) {
            ((WindowDecorActionBar) actionBar).setAnimationEnable(enabled);
        }
    }

    public static void setShoudTransition(ActionBar actionBar, boolean enabled) {
        if (actionBar instanceof WindowDecorActionBar) {
            ((WindowDecorActionBar) actionBar).setShoudTransition(enabled);
        }
    }

    public static void setScrollTabAnimEnable(ActionBar actionBar, boolean enabled) {
        if (actionBar instanceof WindowDecorActionBar) {
            ((WindowDecorActionBar) actionBar).setScrollTabAnimEnable(enabled);
        }
    }
}
