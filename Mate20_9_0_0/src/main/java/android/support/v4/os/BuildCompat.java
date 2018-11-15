package android.support.v4.os;

import android.os.Build.VERSION;

public class BuildCompat {
    private BuildCompat() {
    }

    @Deprecated
    public static boolean isAtLeastN() {
        return VERSION.SDK_INT >= 24;
    }

    @Deprecated
    public static boolean isAtLeastNMR1() {
        return VERSION.SDK_INT >= 25;
    }

    @Deprecated
    public static boolean isAtLeastO() {
        return VERSION.SDK_INT >= 26;
    }

    @Deprecated
    public static boolean isAtLeastOMR1() {
        return VERSION.SDK_INT >= 27;
    }

    public static boolean isAtLeastP() {
        return VERSION.SDK_INT >= 28;
    }

    public static boolean isAtLeastQ() {
        if (VERSION.CODENAME.length() != 1 || VERSION.CODENAME.charAt(0) < 'Q' || VERSION.CODENAME.charAt(0) > 'Z') {
            return false;
        }
        return true;
    }
}
