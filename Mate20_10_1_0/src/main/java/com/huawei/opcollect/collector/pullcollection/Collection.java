package com.huawei.opcollect.collector.pullcollection;

import android.content.Context;
import android.os.Build;
import android.telephony.MSimTelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.android.telephony.TelephonyManagerEx;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.Locale;

public final class Collection {
    private static final int FIRST_SLOT = 0;
    private static final int INVALID_SUBSCRIPTION_ID = -1;
    private static final int SECOND_SLOT = 1;
    private static final String TAG = "CollectionImp";
    private static final String UNKNOWN = "unknown";

    public String getLanguage() {
        Locale locale = Locale.getDefault();
        return "{language:" + locale.getLanguage() + ",country:" + locale.getCountry() + "}";
    }

    public String getHardwareVersion() {
        return SystemPropertiesEx.get("ro.hardware", UNKNOWN);
    }

    public String getDeviceName() {
        return Build.MODEL;
    }

    public String getBuildNumber() {
        return Build.DISPLAY;
    }

    public String getDefaultDataSlotIMSI(Context context) {
        int mainSlot = SubscriptionManager.getDefaultDataSubscriptionId();
        if (mainSlot == -1) {
            mainSlot = 0;
        }
        OPCollectLog.r(TAG, "slot: " + mainSlot);
        return getIMSI(context, mainSlot);
    }

    private String getIMSI(Context context, int slotId) {
        if (context == null) {
            OPCollectLog.e(TAG, "context is null.");
            return "";
        }
        TelephonyManager manager = (TelephonyManager) context.getSystemService("phone");
        if (manager == null) {
            OPCollectLog.e(TAG, "Get TelephonyManager failed.");
            return "";
        }
        String imsi = TelephonyManagerEx.getSubscriberId(manager, slotId);
        return imsi == null ? "" : imsi;
    }

    public String getAllPhoneNumber(Context context) {
        if (context == null) {
            OPCollectLog.e(TAG, "context is null.");
            return "";
        }
        MSimTelephonyManager mSimTelephonyManager = new MSimTelephonyManager(context);
        String firstPhoneNumber = mSimTelephonyManager.getLine1Number(0);
        String secondPhoneNumber = mSimTelephonyManager.getLine1Number(1);
        if (firstPhoneNumber == null && secondPhoneNumber == null) {
            return "";
        }
        if (firstPhoneNumber != null) {
            return secondPhoneNumber == null ? firstPhoneNumber : firstPhoneNumber + "," + secondPhoneNumber;
        }
        return secondPhoneNumber;
    }
}
