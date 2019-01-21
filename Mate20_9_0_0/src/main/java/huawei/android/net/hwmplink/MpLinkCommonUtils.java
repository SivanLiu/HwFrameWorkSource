package huawei.android.net.hwmplink;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.FreezeScreenScene;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.text.TextUtils;
import java.util.List;

public class MpLinkCommonUtils {
    public static final String KEY_MP_LINK_LOG = "ro.config.mplink_log";
    public static final String KEY_MP_LINK_PROPERTY = "ro.config.mplink_enable";
    public static final String KEY_MP_LINK_TEST = "ro.config.mplink_test";
    public static final String KEY_PROP_LOCALE = "ro.product.locale.region";
    public static final String KEY_WIFI_PRO_PROPERTY = "ro.config.hw_wifipro_enable";
    public static final String KEY_WIFI_PRO_SWITCH = "smart_network_switching";
    public static final String SETTING_MPLINK_DB_CONDITION_VALUE = "mplink_db_condition_value";
    public static final String SETTING_MPLINK_MOBILE_SWITCH = "mobile_data";
    public static final String SETTING_MPLINK_SIMULATE_HIBRAIN_REQUEST_FOR_TEST = "mplink_simulate_hibrain_request_for_test";
    public static final String SETTING_SECURE_VPN_WORK_VALUE = "wifipro_network_vpn_state";
    public static final String TAG = "HiData_HiDATA_MpLinkCommonUtils";

    public static boolean isMpLinkEnabled(Context context) {
        if (context != null && getSettingsSystemBoolean(context.getContentResolver(), "smart_network_switching", false) && isSupportMpLink()) {
            return true;
        }
        return false;
    }

    public static boolean isMpLinkEnabledInternal(Context context) {
        return context != null && getSettingsSystemBoolean(context.getContentResolver(), "mplink_db_condition_value", false);
    }

    public static boolean isSupportMpLink() {
        return SystemProperties.getBoolean("ro.config.mplink_enable", true);
    }

    public static boolean isMpLinkTestMode() {
        return SystemProperties.getBoolean("ro.config.mplink_test", false);
    }

    public static String getProductLocale() {
        return SystemProperties.get("ro.product.locale.region", "");
    }

    public static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return System.getInt(cr, name, def) == 1;
    }

    public static boolean getSettingsGlobalBoolean(ContentResolver cr, String name, boolean def) {
        return Global.getInt(cr, name, def) == 1;
    }

    public static void logD(String tag, String log) {
        HwHiDataCommonUtils.logD(tag, log);
    }

    public static void logI(String tag, String log) {
        HwHiDataCommonUtils.logI(tag, log);
    }

    public static int getForegroundAppUid(Context context) {
        if (context == null) {
            return -1;
        }
        List<RunningAppProcessInfo> lr = ((ActivityManager) context.getSystemService(FreezeScreenScene.ACTIVITY_PARAM)).getRunningAppProcesses();
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

    public static String getPackageName(Context context, int uid) {
        if (uid == -1 || context == null) {
            return "total";
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return "total";
        }
        String name = pm.getNameForUid(uid);
        if (TextUtils.isEmpty(name)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown:");
            stringBuilder.append(uid);
            name = stringBuilder.toString();
        }
        return name;
    }

    public static int getAppUid(Context context, String processName) {
        int uid = -1;
        if (TextUtils.isEmpty(processName) || context == null) {
            return -1;
        }
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(processName, 1);
                if (ai != null) {
                    uid = ai.uid;
                }
            } catch (NameNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NameNotFoundException: ");
                stringBuilder.append(e.getMessage());
                logD(str, stringBuilder.toString());
            }
        }
        return uid;
    }

    public static int getNetworkType(Context context, int netId) {
        if (context == null || netId <= 0) {
            return -1;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        Network[] networks = cm.getAllNetworks();
        if (networks != null && networks.length > 0) {
            int length = networks.length;
            for (int i = 0; i < length; i++) {
                NetworkInfo netInfo = cm.getNetworkInfo(networks[i]);
                if (netInfo != null) {
                    Network network = networks[i];
                    if (network != null && network.netId == netId) {
                        return netInfo.getType();
                    }
                }
            }
        }
        return -1;
    }
}
