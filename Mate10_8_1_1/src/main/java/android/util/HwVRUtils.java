package android.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.display.DisplayManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.Display;
import android.view.DisplayInfo;
import java.util.HashSet;

public final class HwVRUtils {
    private static final String ANDROID_SYSTEM = "android";
    private static final boolean IS_VR_ENABLE = SystemProperties.getBoolean("ro.vr.mode", false);
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String TAG = "HwVRUtils#";
    public static final int VR_DISPLAY_HEIGHT = 1600;
    public static final int VR_DISPLAY_WIDTH = 2880;
    public static final int VR_DYNAMIC_STACK_ID = 20007;
    private static final String VR_INSTALL = "com.huawei.vrinstaller";
    public static final String VR_LAUNCHER_PACKAGE = "com.huawei.vrlauncherx";
    private static final String VR_METADATA_NAME = "com.huawei.android.vr.application.mode";
    private static final String VR_METADATA_VALUE = "vr_only";
    private static final String VR_SERVICE = "com.huawei.vrservice";
    public static final String VR_VIRTUAL_SCREEN = "HW-VR-Virtual-Screen";
    public static final String VR_VIRTUAL_SCREEN_PACKAGE = "com.huawei.vrvirtualscreen";
    private static int sDisplayID = -1;
    private static boolean sIsVrMode = false;
    private static String sTargetPackageName = null;
    private static HashSet<String> sVRLowPowerList = new HashSet();

    public static boolean isVRDynamicStack(int stackId) {
        return IS_VR_ENABLE && stackId >= VR_DYNAMIC_STACK_ID;
    }

    public static boolean isVRMode() {
        return IS_VR_ENABLE ? sIsVrMode : false;
    }

    public static void setVRDisplayID(int displayid, boolean vrmode) {
        if (IS_VR_ENABLE) {
            sDisplayID = displayid;
            sIsVrMode = vrmode;
        }
    }

    public static void setTarget(ComponentName componentName) {
        if (componentName != null) {
            sTargetPackageName = componentName.getPackageName();
        } else {
            sTargetPackageName = null;
        }
    }

    public static int getVRDisplayID(Context context) {
        if (!IS_VR_ENABLE || context == null) {
            return -1;
        }
        if (VR_INSTALL.equals(sTargetPackageName)) {
            setTarget(null);
            return -1;
        }
        String packageName = context.getPackageName();
        if ((!isVRMode() && (VR_SERVICE.equals(packageName) ^ 1) != 0) || "android".equals(packageName)) {
            return -1;
        }
        if (isVRApp(context)) {
            DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
            if (displayManager == null) {
                return -1;
            }
            Display[] displays = displayManager.getDisplays();
            for (int i = 1; i < displays.length; i++) {
                Display display = displays[i];
                DisplayInfo disInfo = new DisplayInfo();
                if (display != null && display.getDisplayInfo(disInfo) && isVRDisplay(display.getDisplayId(), disInfo.getNaturalWidth(), disInfo.getNaturalHeight())) {
                    int displayID = display.getDisplayId();
                    setVRDisplayID(displayID, true);
                    return displayID;
                }
            }
            setVRDisplayID(-1, false);
            return getVRDisplayID();
        }
        setVRDisplayID(-1, false);
        return -1;
    }

    public static int getVRDisplayID() {
        if (IS_VR_ENABLE) {
            return sDisplayID;
        }
        return -1;
    }

    public static boolean isValidVRDisplayId(int displayid) {
        boolean z = false;
        if (!IS_VR_ENABLE) {
            return false;
        }
        if (!(displayid == -1 || displayid == 0 || displayid != sDisplayID)) {
            z = true;
        }
        return z;
    }

    public static boolean isVRDisplay(int displayid, int width, int height) {
        if (displayid == -1 || displayid == 0) {
            return false;
        }
        if ((width == VR_DISPLAY_WIDTH && height == 1600) || (width == 1600 && height == VR_DISPLAY_WIDTH)) {
            return true;
        }
        return false;
    }

    public static void addVRLowPowerAppList(String packageName) {
        if (packageName != null && !packageName.trim().equals("") && !packageName.equals("com.android.systemui") && !sVRLowPowerList.contains(packageName)) {
            sVRLowPowerList.add(packageName);
        }
    }

    public static boolean isVRLowPowerApp(String packageName) {
        return sVRLowPowerList.contains(packageName);
    }

    public static int log(String subTag, String msg) {
        String tag = TAG;
        if (!TextUtils.isEmpty(subTag)) {
            tag = tag + subTag;
        }
        return Log.i(tag, msg);
    }

    private static boolean isVRApp(Context context) {
        if (context == null) {
            return false;
        }
        String packageName = context.getPackageName();
        if (packageName == null) {
            return false;
        }
        if ("com.android.systemui".equals(packageName)) {
            return true;
        }
        String vrOnly = "";
        ApplicationInfo appinfo = null;
        try {
            appinfo = context.getPackageManager().getApplicationInfo(packageName, 128);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "getApplicationInfo exception ", e);
        }
        if (!(appinfo == null || appinfo.metaData == null)) {
            vrOnly = appinfo.metaData.getString(VR_METADATA_NAME);
        }
        if (VR_METADATA_VALUE.equals(vrOnly)) {
            return true;
        }
        Log.w(TAG, "no vr app metadata " + vrOnly);
        return false;
    }
}
