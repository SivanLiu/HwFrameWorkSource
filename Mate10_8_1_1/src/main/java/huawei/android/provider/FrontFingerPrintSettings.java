package huawei.android.provider;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.os.SystemProperties;
import android.provider.Settings;
import huawei.android.os.HwGeneralManager;
import huawei.android.provider.HwSettings.System;

public class FrontFingerPrintSettings {
    public static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    public static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
    public static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY_MODE = SystemProperties.getInt("ro.config.front_fp_trikey_mode", 0);
    public static final int SINGLE_VIRTUAL_AI_MODE = SystemProperties.getInt("ro.config.show_navbar_slide", SystemProperties.get("ro.config.hw_optb", System.FINGERSENSE_KNUCKLE_GESTURE_OFF).equals("156") ? 1 : 0);
    public static final int SINGLE_VIRTUAL_NAVIGATION_MODE = SystemProperties.getInt("ro.config.single_virtual_navbar", 0);

    public static boolean isNaviBarEnabled(ContentResolver resolver) {
        boolean z = true;
        int NAVI_BAR_DEFAULT_STATUS = 1;
        if (!FRONT_FINGERPRINT_NAVIGATION) {
            return true;
        }
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
            if (!isChinaArea()) {
                NAVI_BAR_DEFAULT_STATUS = 1;
            } else if (SINGLE_VIRTUAL_NAVIGATION_MODE == 1) {
                NAVI_BAR_DEFAULT_STATUS = 1;
            } else {
                NAVI_BAR_DEFAULT_STATUS = 0;
            }
        } else if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
            return false;
        }
        if (Settings.System.getIntForUser(resolver, System.NAVIGATION_BAR_ENABLE, NAVI_BAR_DEFAULT_STATUS, ActivityManager.getCurrentUser()) != 1) {
            z = false;
        }
        return z;
    }

    public static boolean isSingleVirtualNavbarEnable(ContentResolver resolver) {
        boolean z = true;
        if (!isNaviBarEnabled(resolver)) {
            return false;
        }
        int defaultValue;
        if (SINGLE_VIRTUAL_NAVIGATION_MODE == 1) {
            defaultValue = 1;
        } else {
            defaultValue = 0;
        }
        if (Settings.System.getIntForUser(resolver, System.SINGLE_VIRTUAL_NAVBAR, defaultValue, ActivityManager.getCurrentUser()) != 1) {
            z = false;
        }
        return z;
    }

    public static boolean isSingleNavBarAIEnable(ContentResolver resolver) {
        boolean z = true;
        if (!isSingleVirtualNavbarEnable(resolver)) {
            return false;
        }
        if (Settings.System.getIntForUser(resolver, System.AI_ENTRANCE, SINGLE_VIRTUAL_AI_MODE == 1 ? 1 : 0, ActivityManager.getCurrentUser()) != 1) {
            z = false;
        }
        return z;
    }

    public static int getDefaultNaviMode() {
        if (!isSupportTrikey()) {
            return -1;
        }
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
            return -1;
        }
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1) {
            return -1;
        }
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY_MODE == 1) {
            return 0;
        }
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY_MODE == 2) {
            return -1;
        }
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY_MODE == 3) {
            if (isChinaArea()) {
                return 0;
            }
            return -1;
        } else if (isChinaArea()) {
            return -1;
        } else {
            return 0;
        }
    }

    public static int getDefaultBtnLightMode() {
        return SystemProperties.getInt("ro.config.front_fp_btnlight", 1);
    }

    public static boolean isSupportTrikey() {
        return HwGeneralManager.getInstance().isSupportTrikey();
    }

    public static boolean isChinaArea() {
        return SystemProperties.get("ro.config.hw_optb", System.FINGERSENSE_KNUCKLE_GESTURE_OFF).equals("156");
    }
}
