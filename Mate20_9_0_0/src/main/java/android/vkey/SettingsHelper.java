package android.vkey;

import android.content.Context;
import android.provider.Settings.System;

public class SettingsHelper {
    private static final int TOUCH_PLUS_ON = 1;

    public static boolean isTouchPlusOn(Context ctx) {
        int tplus = System.getInt(ctx.getContentResolver(), "hw_membrane_touch_enabled", 0);
        int tplusnbaron = System.getInt(ctx.getContentResolver(), "hw_membrane_touch_navbar_enabled", 0);
        if (1 == tplus && 1 == tplusnbaron) {
            return true;
        }
        return false;
    }
}
