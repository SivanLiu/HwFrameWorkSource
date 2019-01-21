package huawei.android.provider;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.Flog;
import huawei.android.app.admin.ConstantValue;

public class FingerSenseSettings {
    public static final String INTENT_EXTRA_STARTFLG = "startflg";
    public static final String INTENT_EXTRA_STARTFLG_EASYWAKEUP = "easywakeup";
    private static final String MULTISCREENSHOT_ACTION = "com.huawei.HwMultiScreenShot.start";
    private static final String MULTISCREENSHOT_PREFIX = "com.huawei.HwMultiScreenShot";
    private static final String MULTISCREENSHOT_SERVICE = "com.huawei.HwMultiScreenShot.MultiScreenShotService";
    public static final String SCREENSHOT_REGION_INTENT = "com.qeexo.smartshot.CropActivity";
    private static boolean isDrawGestureEnabled = false;
    private static boolean isLineGestureEnabled = false;
    private static boolean isSmartshotEnabled = false;
    private static String mRunmode = SystemProperties.get("ro.runmode", "normal");

    public static synchronized void updateSmartshotEnabled(ContentResolver resolver) {
        synchronized (FingerSenseSettings.class) {
            boolean z = true;
            if (System.getIntForUser(resolver, HwSettings.System.FINGERSENSE_SMARTSHOT_ENABLED, 1, ActivityManager.getCurrentUser()) != 1) {
                z = false;
            }
            isSmartshotEnabled = z;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSmartshotEnabled set to ");
            stringBuilder.append(isSmartshotEnabled);
            Flog.i(ConstantValue.transaction_isRooted, stringBuilder.toString());
            updateFingerSenseEnable(resolver);
        }
    }

    public static synchronized void updateLineGestureEnabled(ContentResolver resolver) {
        synchronized (FingerSenseSettings.class) {
            boolean z = true;
            if (System.getIntForUser(resolver, HwSettings.System.FINGERSENSE_LINE_GESTURE_ENABLED, 1, ActivityManager.getCurrentUser()) != 1) {
                z = false;
            }
            isLineGestureEnabled = z;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateLineGestureEnabled set to ");
            stringBuilder.append(isLineGestureEnabled);
            Flog.i(ConstantValue.transaction_isRooted, stringBuilder.toString());
            updateFingerSenseEnable(resolver);
        }
    }

    public static synchronized void updateDrawGestureEnabled(ContentResolver resolver) {
        synchronized (FingerSenseSettings.class) {
            boolean z = true;
            if (System.getIntForUser(resolver, HwSettings.System.FINGERSENSE_LETTERS_ENABLED, 1, ActivityManager.getCurrentUser()) != 1) {
                z = false;
            }
            isDrawGestureEnabled = z;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateDrawGestureEnabled set to ");
            stringBuilder.append(isDrawGestureEnabled);
            Flog.i(ConstantValue.transaction_isRooted, stringBuilder.toString());
            updateFingerSenseEnable(resolver);
        }
    }

    public static synchronized void updateFingerSenseEnable(ContentResolver resolver) {
        synchronized (FingerSenseSettings.class) {
            if (!(isSmartshotEnabled || isDrawGestureEnabled)) {
                if (!isLineGestureEnabled) {
                    Global.putInt(resolver, HwSettings.System.FINGERSENSE_ENABLED, 0);
                }
            }
            Global.putInt(resolver, HwSettings.System.FINGERSENSE_ENABLED, 1);
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0023, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:14:0x0025, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized boolean isFingerSenseEnabled(ContentResolver resolver) {
        synchronized (FingerSenseSettings.class) {
            boolean z = false;
            if (!"factory".equals(mRunmode)) {
                if (!SystemProperties.getBoolean("sys.super_power_save", false)) {
                    if (Global.getInt(resolver, HwSettings.System.FINGERSENSE_ENABLED, 1) == 1) {
                        z = true;
                    }
                }
            }
        }
    }

    public static boolean isFingerSenseSmartshotEnabled(ContentResolver resolver) {
        return System.getIntForUser(resolver, HwSettings.System.FINGERSENSE_SMARTSHOT_ENABLED, 1, ActivityManager.getCurrentUser()) == 1;
    }

    public static boolean isFingerSenseDoubleKnuckleEnabled(ContentResolver resolver) {
        return System.getIntForUser(resolver, HwSettings.System.FINGERSENSE_DOUBLE_KNUCKLE_ENABLED, 1, ActivityManager.getCurrentUser()) == 1;
    }

    public static boolean areDrawGesturesEnabled(ContentResolver resolver) {
        return System.getIntForUser(resolver, HwSettings.System.FINGERSENSE_LETTERS_ENABLED, 1, ActivityManager.getCurrentUser()) == 1;
    }

    public static boolean isKnuckleGestureEnable(String gestureName, ContentResolver resolver) {
        if (gestureName.equals(HwSettings.System.FINGERSENSE_KNUCKLE_GESTURE_REGION_SUFFIX)) {
            return isFingerSenseSmartshotEnabled(resolver);
        }
        if (areDrawGesturesEnabled(resolver)) {
            return isValidLetterGestureAppInfo(getLetterGestureAppInfo(gestureName, resolver));
        }
        return false;
    }

    public static boolean isFingerSenseLineGestureEnabled(ContentResolver resolver) {
        return System.getIntForUser(resolver, HwSettings.System.FINGERSENSE_LINE_GESTURE_ENABLED, 1, ActivityManager.getCurrentUser()) == 1;
    }

    private static boolean isValidLetterGestureAppInfo(String[] appInfo) {
        boolean z = false;
        if (appInfo == null || appInfo.length != 2) {
            return false;
        }
        if (!(appInfo[0].equals("null") || appInfo[1].equals("null"))) {
            z = true;
        }
        return z;
    }

    private static String[] getLetterGestureAppInfo(String gestureName, ContentResolver resolver) {
        String gestureKey = new StringBuilder();
        gestureKey.append(HwSettings.System.EASYFINGER_LETTER_SETTING_PREFIX);
        gestureKey.append(gestureName);
        String gestureValue = System.getStringForUser(resolver, gestureKey.toString(), ActivityManager.getCurrentUser());
        if (gestureValue == null) {
            return new String[0];
        }
        return gestureValue.split(";");
    }

    public static Intent getIntentForGesture(String gestureName, Context context) {
        if (gestureName.equals(HwSettings.System.FINGERSENSE_KNUCKLE_GESTURE_REGION_SUFFIX)) {
            return new Intent(SCREENSHOT_REGION_INTENT);
        }
        String[] appInfo = getLetterGestureAppInfo(gestureName, context.getContentResolver());
        if (!isValidLetterGestureAppInfo(appInfo)) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setClassName(appInfo[0], appInfo[1]);
        intent.putExtra(INTENT_EXTRA_STARTFLG, INTENT_EXTRA_STARTFLG_EASYWAKEUP);
        return intent;
    }

    public static Intent getIntentForMultiScreenShot(Context context) {
        Intent startIntent = new Intent(MULTISCREENSHOT_ACTION);
        startIntent.setClassName(MULTISCREENSHOT_PREFIX, MULTISCREENSHOT_SERVICE);
        return startIntent;
    }
}
