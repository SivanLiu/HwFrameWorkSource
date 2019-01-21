package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HwQoE.HiDataTracfficInfo.HiDataApInfo;

public class HiDataApBlackWhiteListManager {
    public static final String TAG = "HiDATA_WhiteListManager";
    private static HiDataApBlackWhiteListManager mHiDataApBlackWhiteListManager;
    private Context mContext;
    private String mCurrentDefaultDataImsi;
    private HwQoEQualityManager mHwQoEQualityManager = HwQoEQualityManager.getInstance(this.mContext);
    private TelephonyManager mTelephonyManager = ((TelephonyManager) this.mContext.getSystemService("phone"));

    public static HiDataApBlackWhiteListManager createInstance(Context context) {
        if (mHiDataApBlackWhiteListManager == null) {
            mHiDataApBlackWhiteListManager = new HiDataApBlackWhiteListManager(context);
        }
        return mHiDataApBlackWhiteListManager;
    }

    private HiDataApBlackWhiteListManager(Context context) {
        this.mContext = context;
    }

    public synchronized void addHandoverWhiteList(String ssid, int apAuthType, int appType) {
        if (!TextUtils.isEmpty(ssid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addHandoverWhiteList,ssid: ");
            stringBuilder.append(ssid);
            logD(stringBuilder.toString());
            this.mHwQoEQualityManager.addOrUpdateAPRcd(new HiDataApInfo(ssid, apAuthType, 1, appType, 0));
        }
    }

    public synchronized void addHandoverBlackList(String ssid, int apAuthType, int appType) {
        if (!TextUtils.isEmpty(ssid)) {
            HiDataApInfo hiDataApInfo = this.mHwQoEQualityManager.queryAPUseType(ssid, apAuthType, appType);
            StringBuilder stringBuilder;
            if (2 <= hiDataApInfo.mBlackCount) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("addHandoverBlackList,max blackCount ,ignore  ");
                stringBuilder.append(ssid);
                logD(stringBuilder.toString());
                return;
            }
            hiDataApInfo = new HiDataApInfo(ssid, apAuthType, 2, appType, hiDataApInfo.mBlackCount + 1);
            stringBuilder = new StringBuilder();
            stringBuilder.append("addHandoverBlackList,ssid: ");
            stringBuilder.append(ssid);
            stringBuilder.append(" , count : ");
            stringBuilder.append(hiDataApInfo.mBlackCount);
            logD(stringBuilder.toString());
            this.mHwQoEQualityManager.addOrUpdateAPRcd(hiDataApInfo);
        }
    }

    private int getBlackListCounter(String ssid, int apAuthType, int appType) {
        if (TextUtils.isEmpty(ssid)) {
            return 0;
        }
        int counter = 0;
        HiDataApInfo hiDataApInfo = this.mHwQoEQualityManager.queryAPUseType(ssid, apAuthType, appType);
        if (2 == hiDataApInfo.mApType) {
            counter = hiDataApInfo.mBlackCount;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getBlackListCounter,ssid: ");
        stringBuilder.append(ssid);
        stringBuilder.append(", counter :");
        stringBuilder.append(counter);
        logD(stringBuilder.toString());
        return counter;
    }

    /* JADX WARNING: Missing block: B:22:0x003b, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isInTheBlackList(String ssid, int apAuthType, int appType, int usertype) {
        if (TextUtils.isEmpty(ssid)) {
            return false;
        }
        int counter = getBlackListCounter(ssid, apAuthType, appType);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isInTheBlackList,usertype: ");
        stringBuilder.append(usertype);
        stringBuilder.append(", black counter: ");
        stringBuilder.append(counter);
        logD(stringBuilder.toString());
        if (counter <= 0) {
            return false;
        }
        if (1 == usertype) {
            return true;
        }
        if (usertype == 0 && counter >= 2) {
            return true;
        }
    }

    public synchronized boolean isInTheWhiteList(String ssid, int apAuthType, int appType) {
        if (TextUtils.isEmpty(ssid)) {
            return false;
        }
        if (1 != this.mHwQoEQualityManager.queryAPUseType(ssid, apAuthType, appType).mApType) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ssid);
        stringBuilder.append(" in the WhiteList ");
        logD(stringBuilder.toString());
        return true;
    }

    /* JADX WARNING: Missing block: B:28:0x0071, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int getApBlackType(String ssid, int apAuthType, int appType, int usertype) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApBlackType,ssid: ");
        stringBuilder.append(ssid);
        stringBuilder.append(", apAuthType: ");
        stringBuilder.append(apAuthType);
        stringBuilder.append(", appType: ");
        stringBuilder.append(appType);
        stringBuilder.append(" ,usertype:");
        stringBuilder.append(usertype);
        logD(stringBuilder.toString());
        if (TextUtils.isEmpty(ssid)) {
            return 0;
        }
        HiDataApInfo hiDataApInfo = this.mHwQoEQualityManager.queryAPUseType(ssid, apAuthType, appType);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("hiDataApInfo:");
        stringBuilder2.append(hiDataApInfo.toString());
        logD(stringBuilder2.toString());
        if (1 == hiDataApInfo.mApType) {
            return 1;
        }
        if (2 == hiDataApInfo.mApType) {
            if (1 == usertype && hiDataApInfo.mBlackCount >= 1) {
                return 2;
            }
            if (usertype == 0) {
                if (hiDataApInfo.mBlackCount >= 2) {
                    return 2;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0038, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:0x003a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updateHoldWiFiCounter(String ssid, int apAuthType, int appType) {
        if (!TextUtils.isEmpty(ssid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateHoldWiFiCounter,ssid: ");
            stringBuilder.append(ssid);
            logD(stringBuilder.toString());
            if (!TextUtils.isEmpty(getCurrentDefaultDataImsi())) {
                if (!isInTheWhiteList(ssid, apAuthType, appType)) {
                    if (getBlackListCounter(ssid, apAuthType, appType) > 0) {
                        minusHandoverBlackCounter(ssid, apAuthType, appType);
                    }
                }
            }
        }
    }

    private void minusHandoverBlackCounter(String ssid, int apAuthType, int appType) {
        if (!TextUtils.isEmpty(ssid)) {
            HiDataApInfo hiDataApInfo = this.mHwQoEQualityManager.queryAPUseType(ssid, apAuthType, appType);
            if (2 == hiDataApInfo.mApType && hiDataApInfo.mBlackCount > 0) {
                hiDataApInfo.mBlackCount--;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("minusHandoverBlackCounter,ssid: ");
                stringBuilder.append(ssid);
                stringBuilder.append(", count: ");
                stringBuilder.append(hiDataApInfo.mBlackCount);
                logD(stringBuilder.toString());
                this.mHwQoEQualityManager.addOrUpdateAPRcd(hiDataApInfo);
            }
        }
    }

    private String getCurrentDefaultDataImsi() {
        this.mCurrentDefaultDataImsi = this.mTelephonyManager.getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
        return this.mCurrentDefaultDataImsi;
    }

    private void logD(String info) {
        Log.d(TAG, info);
    }
}
