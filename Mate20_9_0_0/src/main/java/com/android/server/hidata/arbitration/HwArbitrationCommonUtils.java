package com.android.server.hidata.arbitration;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.List;

public class HwArbitrationCommonUtils {
    private static final String KEY_HiCure_Show = "ro.config.hw_hicure_test";
    public static final boolean MAINLAND_REGION = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    public static final String TAG = "HiData_HwArbitrationCommonUtils";

    public static void logD(String tag, String log) {
        Log.d(tag, log);
    }

    public static void logI(String tag, String log) {
        Log.i(tag, log);
    }

    public static void logE(String tag, String log) {
        Log.e(tag, log);
    }

    public static int getActiveConnectType(Context mContext) {
        if (mContext == null) {
            logE(TAG, "getActiveConnectType: mContext is null");
            return 802;
        }
        NetworkInfo activeNetInfo = ((ConnectivityManager) mContext.getSystemService("connectivity")).getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == 1 && activeNetInfo.isConnected()) {
            logD(TAG, "TYPE_WIFI is active");
            return 800;
        } else if (activeNetInfo != null && activeNetInfo.getType() == 0 && activeNetInfo.isConnected()) {
            logD(TAG, "TYPE_MOBILE is active");
            return 801;
        } else {
            logD(TAG, "ACTIVE_TYPE is none");
            return 802;
        }
    }

    public static boolean isWifiEnabled(Context mContext) {
        return Global.getInt(mContext.getContentResolver(), "wifi_on", 0) != 0;
    }

    public static boolean isCellConnected(Context mContext) {
        NetworkInfo cellNetInfo = ((ConnectivityManager) mContext.getSystemService("connectivity")).getNetworkInfo(0);
        if (cellNetInfo == null || cellNetInfo.getState() != State.CONNECTED) {
            return false;
        }
        return true;
    }

    public static boolean isWifiConnected(Context mContext) {
        NetworkInfo wifiNetInfo = ((ConnectivityManager) mContext.getSystemService("connectivity")).getNetworkInfo(1);
        if (wifiNetInfo == null || wifiNetInfo.getState() != State.CONNECTED) {
            return false;
        }
        return true;
    }

    public static boolean isCellEnable(Context mContext) {
        return Global.getInt(mContext.getContentResolver(), "mobile_data", 0) != 0;
    }

    public static boolean isDataRoamingEnable(Context mContext) {
        boolean z = false;
        if (Global.getInt(mContext.getContentResolver(), "data_roaming", 0) != 0) {
            z = true;
        }
        boolean result = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DataRoamingStateEnable:");
        stringBuilder.append(result);
        logD(str, stringBuilder.toString());
        return result;
    }

    public static boolean hasSimCard(Context mContext) {
        if (((TelephonyManager) mContext.getSystemService("phone")).getSimState() != 5) {
            return false;
        }
        return true;
    }

    public static boolean isScreenOn(Context mContext) {
        for (Display display : ((DisplayManager) mContext.getSystemService("display")).getDisplays()) {
            if (display.getState() == 2 || display.getState() == 0) {
                logD(TAG, "display STATE is ON");
                return true;
            }
        }
        return false;
    }

    public static boolean isVicePhoneCalling(Context context) {
        boolean isCalling = false;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        int viceSubId = 0;
        if (telephonyManager == null) {
            logE(TAG, "isVicePhoneCalling: telephonyManager is null, return!");
            return false;
        }
        if (2 == telephonyManager.getPhoneCount()) {
            if (SubscriptionManager.getDefaultSubId() == 0) {
                viceSubId = 1;
            }
            if (5 == telephonyManager.getSimState(viceSubId) && (2 == telephonyManager.getCallState(viceSubId) || 1 == telephonyManager.getCallState(viceSubId))) {
                isCalling = true;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isViceSIMCalling:");
        stringBuilder.append(isCalling);
        logD(str, stringBuilder.toString());
        return isCalling;
    }

    public static boolean isDefaultPhoneCSCalling(Context context) {
        boolean isCSCalling = false;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        String str;
        StringBuilder stringBuilder;
        if (telephonyManager == null) {
            logE(TAG, "isDefaultPhoneCSCalling: telephonyManager is null, return!");
            return false;
        } else if (telephonyManager.isVolteAvailable()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isVolteAvailable:");
            stringBuilder.append(telephonyManager.isVolteAvailable());
            logD(str, stringBuilder.toString());
            return false;
        } else {
            int mDefaultSubId = SubscriptionManager.getDefaultSubId();
            if (5 == telephonyManager.getSimState(mDefaultSubId) && (2 == telephonyManager.getCallState(mDefaultSubId) || 1 == telephonyManager.getCallState(mDefaultSubId))) {
                isCSCalling = true;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isDefaultSIMCSCalling:");
            stringBuilder.append(isCSCalling);
            logD(str, stringBuilder.toString());
            return isCSCalling;
        }
    }

    public static boolean isSlotIdValid(int slotId) {
        return slotId >= 0 && 2 > slotId;
    }

    public static boolean isDataConnected(Context context) {
        return 2 == ((TelephonyManager) context.getSystemService("phone")).getDataState();
    }

    public static String getTopActivityPackageName(Context context) {
        try {
            List<RunningTaskInfo> tasks = ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1);
            if (tasks == null || tasks.isEmpty()) {
                logD(TAG, "Top_Activity,Null");
                return null;
            }
            ComponentName topActivity = ((RunningTaskInfo) tasks.get(0)).topActivity;
            if (topActivity != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Top_Activity, pgName:");
                stringBuilder.append(topActivity.getPackageName());
                logD(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Top_Activity, className:");
                stringBuilder.append(topActivity.getClassName());
                logD(str, stringBuilder.toString());
                return topActivity.getPackageName();
            }
            return null;
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Debug_Top_Activity:Failure to get topActivity PackageName ");
            stringBuilder2.append(e);
            logD(str2, stringBuilder2.toString());
        }
    }

    public static int getForegroundAppUid(Context context) {
        if (context == null) {
            return -1;
        }
        List<RunningAppProcessInfo> lr = ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
        if (lr == null) {
            return -1;
        }
        for (RunningAppProcessInfo ra : lr) {
            if (ra.importance == 200 || ra.importance == 100) {
                return ra.uid;
            }
        }
        return -1;
    }

    public static boolean isDataRoamingEnabled(Context context, int subId) {
        boolean z = false;
        if (subId < 0 || subId > 1) {
            logD(TAG, "unvalid SubId");
            return false;
        }
        String ROAMING_SIM = "data_roaming";
        if (1 == subId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ROAMING_SIM);
            stringBuilder.append("_sim2");
            ROAMING_SIM = stringBuilder.toString();
        }
        if (Global.getInt(context.getContentResolver(), ROAMING_SIM, 0) != 0) {
            z = true;
        }
        return z;
    }

    public static String getAppNameUid(Context context, int uid) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        String processName = "";
        if (activityManager == null) {
            return processName;
        }
        List<RunningAppProcessInfo> appProcessList = activityManager.getRunningAppProcesses();
        if (appProcessList == null || appProcessList.size() == 0) {
            return processName;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.uid == uid) {
                return appProcess.processName;
            }
        }
        return processName;
    }

    public static boolean isHiCureShow() {
        return SystemProperties.getBoolean(KEY_HiCure_Show, false);
    }
}
