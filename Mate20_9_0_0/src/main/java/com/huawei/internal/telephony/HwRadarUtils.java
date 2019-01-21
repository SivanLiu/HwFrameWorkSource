package com.huawei.internal.telephony;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.util.LogException;

public class HwRadarUtils {
    private static final String APP_VERSION = "1.0";
    private static final String CAT_CSP = "csp";
    private static final String CAT_MMS = "mms";
    private static final String CHR_ACTION = "com.huawei.android.chr.action.ACTION_REPORT_CHR";
    private static final String CHR_RECEIVE_PERMISSION = "com.huawei.android.permission.GET_CHR_DATA";
    private static final String DEFAULT_PACKAGE = "com.android.mms";
    public static final int ERROR_BASE_MMS = 1300;
    public static final int ERR_SMS_RECEIVE = 1312;
    public static final int ERR_SMS_SEND = 1311;
    public static final int ERR_SMS_SEND_BACKGROUND = 1317;
    private static final int MODULE_ID = 5000;
    private static final int RADAR_BUG_TYPE_FUNCTION_ERROR = 100;
    public static final int RADAR_LEVEL_A = 65;
    public static final int RADAR_LEVEL_B = 66;
    public static final int RADAR_LEVEL_C = 67;
    public static final int RADAR_LEVEL_D = 68;
    private static final String TAG = "HwRadarUtils";
    private static LogException mLogException = HwFrameworkFactory.getLogException();
    private static final String sAppInfo;

    static {
        StringBuilder stringBuilder = new StringBuilder(256);
        stringBuilder.append("Package: ");
        stringBuilder.append(DEFAULT_PACKAGE);
        stringBuilder.append("\n");
        stringBuilder.append("APK version: ");
        stringBuilder.append(APP_VERSION);
        stringBuilder.append("\n");
        sAppInfo = stringBuilder.toString();
    }

    private HwRadarUtils() {
    }

    public static void report(int errorType, String content) {
        report(null, errorType, content, 0);
    }

    public static void report(int errorType, String content, int subId) {
        report(null, errorType, content, subId);
    }

    public static void report(Context context, int errorType, String content) {
        report(context, errorType, content, SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    public static void report(Context context, int errorType, String content, int subId) {
        String header = new StringBuilder(256);
        header.append(sAppInfo);
        header.append("Bug type: ");
        header.append(String.valueOf(transalateErrorToBugType(errorType)));
        header.append("\n");
        header.append("Scene def: ");
        header.append(String.valueOf(errorType));
        header.append("\n");
        header = header.toString();
        if (isNeedToTriggerAppRadar(errorType)) {
            reportApplicationRadarLog(header, content);
        }
        if (isNeedToTriggerCHR(errorType)) {
            reportChr(context, errorType, content, subId);
        }
    }

    private static final int transalateErrorToBugType(int error) {
        return 100;
    }

    private static boolean isNeedToTriggerAppRadar(int errorType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNeedToTriggerAppRadar for error:");
        stringBuilder.append(errorType);
        Rlog.d(str, stringBuilder.toString());
        if (errorType != ERR_SMS_SEND_BACKGROUND) {
            switch (errorType) {
                case ERR_SMS_SEND /*1311*/:
                    Rlog.d(TAG, "not need to trigger applicaton radar log");
                    return false;
                case ERR_SMS_RECEIVE /*1312*/:
                    break;
                default:
                    Rlog.d(TAG, "Not need to trigger APP Radar");
                    return false;
            }
        }
        Rlog.d(TAG, "need to trigger applicaton radar log");
        return true;
    }

    private static boolean isNeedToTriggerCHR(int errorType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNeedToTriggerCHR for error:");
        stringBuilder.append(errorType);
        Rlog.d(str, stringBuilder.toString());
        if (errorType == ERR_SMS_RECEIVE || errorType == ERR_SMS_SEND_BACKGROUND) {
            Rlog.d(TAG, "need To TriggerCHR");
            return true;
        }
        Rlog.d(TAG, "not need to trigger CHR");
        return false;
    }

    public static void reportApplicationRadarLog(String header, String msg) {
        reportApplicationRadarLog(CAT_MMS, 65, header, msg);
    }

    public static void reportApplicationRadarLog(String category, int level, String header, String msg) {
        if (mLogException != null) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("radar report in FW:");
                stringBuilder.append(msg);
                Rlog.w(str, stringBuilder.toString());
                mLogException.msg(category, level, header, msg);
                return;
            } catch (Throwable e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("call radar interface has exception");
                stringBuilder2.append(e.getMessage());
                Rlog.e(str2, stringBuilder2.toString());
                return;
            }
        }
        Rlog.e(TAG, "Radar interface is not support");
    }

    private static void reportChr(Context context, int errorType, String content, int subId) {
        if (context == null || -1 == subId) {
            Rlog.e(TAG, "para is error, not to trigger reportChr");
            return;
        }
        Intent intent = new Intent(CHR_ACTION);
        intent.putExtra("module_id", 5000);
        intent.putExtra("event_id", errorType);
        intent.putExtra("sub", subId);
        intent.putExtra("app_data", content);
        context.sendBroadcast(intent, CHR_RECEIVE_PERMISSION);
        Rlog.d(TAG, "reportChr in FW done");
    }
}
