package com.android.server.wm;

import android.os.SystemProperties;

public final class HwWmConstants {
    public static final String DRAWER_LAUNCHER_CMP = "com.huawei.android.launcher/.drawer.DrawerLauncher";
    public static final String FLAG_STR = "flag";
    public static final String HW_LAUNCHER_PKG = "com.huawei.android.launcher";
    public static final String ICON_HEIGHT_STR = "iconHeight";
    public static final int ICON_TYPE_NONE = 0;
    public static final int ICON_TYPE_STARNDRAD = 1;
    public static final String ICON_WIDTH_STR = "iconWidth";
    public static final boolean IS_APP_LOW_PERF_ANIM = SystemProperties.getBoolean("ro.feature.appstart.low_perf_anim", false);
    public static final boolean IS_HW_SUPPORT_LAUNCHER_EXIT_ANIM = (!SystemProperties.getBoolean("ro.config.disable_launcher_exit_anim", false));
    public static final String IS_LANDSCAPE_STR = "isLandscape";
    public static final String LAUNCHER_PKG_NAME_STR = "launcherPkgName";
    public static final String NEW_SIMPLE_LAUNCHER_CMP = "com.huawei.android.launcher/.newsimpleui.NewSimpleLauncher";
    public static final String PERMISSION_DIALOG_CMP = "com.android.packageinstaller/.permission.ui.GrantPermissionsActivity";
    public static final String PIVOTX_STR = "pivotX";
    public static final String PIVOTY_STR = "pivotY";
    public static final String STK_DIALOG_CMP = "com.android.stk/.StkDialogActivity";
    public static final String UNI_LAUNCHER_CMP = "com.huawei.android.launcher/.unihome.UniHomeLauncher";

    private HwWmConstants() {
    }

    public static boolean containsLauncherCmpName(String str) {
        if (str == null) {
            return false;
        }
        if (str.contains(UNI_LAUNCHER_CMP) || str.contains(DRAWER_LAUNCHER_CMP) || str.contains(NEW_SIMPLE_LAUNCHER_CMP)) {
            return true;
        }
        return false;
    }

    public static boolean isLauncherPkgName(String pkgName) {
        return "com.huawei.android.launcher".equals(pkgName);
    }
}
