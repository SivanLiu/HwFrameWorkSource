package com.android.server.gesture;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.TypedValue;
import com.android.server.wifipro.WifiProCommonUtils;

public class GestureNavConst {
    public static final float BACK_WINDOW_HEIGHT_RATIO = 0.75f;
    public static final float BOTTOM_WINDOW_QUICK_STEP_RATIO = 0.6f;
    public static final float BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    public static final int CHECK_AFT_TIMEOUT = 2000;
    public static final boolean CHINA_REGION = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    public static final int COMPENSATE_MOVE_MAX_TIME = 500;
    public static final int COMPENSATE_MOVE_MIN_TIME = 80;
    public static final boolean DEBUG = Log.HWINFO;
    public static final boolean DEBUG_ALL;
    public static final boolean DEBUG_DUMP = Log.HWINFO;
    public static final String DEFAULT_LAUNCHER_DRAWER_WINDOW = "com.huawei.android.launcher/com.huawei.android.launcher.drawer.DrawerLauncher";
    public static final String DEFAULT_LAUNCHER_MAIN_WINDOW = "com.huawei.android.launcher/com.huawei.android.launcher.unihome.UniHomeLauncher";
    public static final String DEFAULT_LAUNCHER_PACKAGE = "com.huawei.android.launcher";
    public static final String DEFAULT_LAUNCHER_SIMPLE_WINDOW = "com.huawei.android.launcher/com.huawei.android.launcher.newsimpleui.NewSimpleLauncher";
    public static final String DEFAULT_QUICKSTEP_CLASS = "com.android.quickstep.RecentsActivity";
    public static final int DEFAULT_VALUE_DISPLAY_NOTCH_STATUS = 0;
    public static final int DEFAULT_VALUE_GESTURE_NAVIGATION = 0;
    public static final int DEFAULT_VALUE_GESTURE_NAVIGATION_ASSISTANT = (CHINA_REGION ? 0 : 1);
    public static final float FAST_VELOCITY_THRESHOLD_1 = 4500.0f;
    public static final float FAST_VELOCITY_THRESHOLD_2 = 6000.0f;
    public static final float GESTURE_BACK_ANIM_MAX_RATIO = 0.88f;
    public static final int GESTURE_GO_HOME_MIN_DISTANCE_THRESHOLD = 180;
    public static final int GESTURE_GO_HOME_TIMEOUT = 500;
    public static final int GESTURE_MOVE_MAX_ANGLE = 70;
    public static final int GESTURE_MOVE_MIN_DISTANCE_THRESHOLD = 15;
    public static final int GESTURE_MOVE_TIME_THRESHOLD_1 = 120;
    public static final int GESTURE_MOVE_TIME_THRESHOLD_2 = 150;
    public static final int GESTURE_MOVE_TIME_THRESHOLD_3 = 200;
    public static final int GESTURE_MOVE_TIME_THRESHOLD_4 = 250;
    public static final int GESTURE_SLIDE_OUT_MAX_ANGLE = 45;
    public static final int ID_GESTURE_NAV_BOTTOM = 3;
    public static final int ID_GESTURE_NAV_LEFT = 1;
    public static final int ID_GESTURE_NAV_RIGHT = 2;
    public static final String KEY_DISPLAY_NOTCH_STATUS = "display_notch_status";
    public static final String KEY_SECURE_GESTURE_NAVIGATION = "secure_gesture_navigation";
    public static final String KEY_SECURE_GESTURE_NAVIGATION_ASSISTANT = "secure_gesture_navigation_assistant";
    public static final int LONG_PRESS_RESTART_TIMEOUT = 150;
    public static final int LONG_PRESS_TIMEOUT = 200;
    public static final int MODE_OFF = 0;
    public static final int MODE_ON = 1;
    public static final int MOVE_INTERVAL_DISTANCE = 150;
    public static final String RECENT_WINDOW = "com.android.systemui/com.android.systemui.recents.RecentsActivity";
    public static final String REPORT_FAILURE = "failure";
    public static final String REPORT_SUCCESS = "success";
    public static final String STARTUP_GUIDE_HOME_COMPONENT = "com.huawei.hwstartupguide/com.huawei.hwstartupguide.LanguageSelectActivity";
    public static final String STATUSBAR_WINDOW = "StatusBar";
    public static final String TAG_GESTURE_BACK = "GestureNav_Back";
    public static final String TAG_GESTURE_BOTTOM = "GestureNav_Bottom";
    public static final String TAG_GESTURE_OPS = "GestureNav_OPS";
    public static final String TAG_GESTURE_QS = "GestureNav_QS";
    public static final String TAG_GESTURE_QSH = "GestureNav_QSH";
    public static final String TAG_GESTURE_QSO = "GestureNav_QSO";
    public static final String TAG_GESTURE_STRATEGY = "GestureNav_Strategy";
    public static final String TAG_GESTURE_UTILS = "GestureNav_Utils";
    public static final boolean USE_ANIM_LEGACY;

    static {
        float f;
        boolean z = false;
        boolean z2 = DEBUG && Log.HWLog;
        DEBUG_ALL = z2;
        if (SystemProperties.getInt("ro.config.curved_screen", 0) == 1) {
            f = 1.8f;
        } else {
            f = 1.4f;
        }
        BOTTOM_WINDOW_SINGLE_HAND_RATIO = f;
        if (VERSION.SDK_INT <= 27) {
            z = true;
        }
        USE_ANIM_LEGACY = z;
    }

    public static String reportResultStr(boolean success, int side) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("result:");
        stringBuilder.append(success ? REPORT_SUCCESS : REPORT_FAILURE);
        stringBuilder.append(",side:");
        stringBuilder.append(side);
        return stringBuilder.toString();
    }

    public static boolean isGestureNavEnabled(Context context, int userHandle) {
        return Secure.getIntForUser(context.getContentResolver(), KEY_SECURE_GESTURE_NAVIGATION, 0, userHandle) == 1;
    }

    public static boolean isSlideOutEnabled(Context context, int userHandle) {
        return Secure.getIntForUser(context.getContentResolver(), KEY_SECURE_GESTURE_NAVIGATION_ASSISTANT, DEFAULT_VALUE_GESTURE_NAVIGATION_ASSISTANT, userHandle) == 1;
    }

    public static int getBackWindowWidth(Context context) {
        return context.getResources().getDimensionPixelSize(34472315);
    }

    public static int getBottomWindowHeight(Context context) {
        return context.getResources().getDimensionPixelSize(34472316);
    }

    public static int getBackMaxDistance1(Context context) {
        return context.getResources().getDimensionPixelSize(34472313);
    }

    public static int getBackMaxDistance2(Context context) {
        return context.getResources().getDimensionPixelSize(34472314);
    }

    public static int getStatusBarHeight(Context context) {
        return context.getResources().getDimensionPixelSize(17105318);
    }

    public static int convertDpToPixel(float dp) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * dp);
    }

    public static int dipToPx(Context context, int dip) {
        return Math.round(TypedValue.applyDimension(1, (float) dip, context.getResources().getDisplayMetrics()));
    }

    public static int mmToPx(Context context, float value) {
        return Math.round(TypedValue.applyDimension(5, value, context.getResources().getDisplayMetrics()));
    }

    public static int getDimensionPixelSize(Context context, int resId) {
        return context.getResources().getDimensionPixelSize(resId);
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
