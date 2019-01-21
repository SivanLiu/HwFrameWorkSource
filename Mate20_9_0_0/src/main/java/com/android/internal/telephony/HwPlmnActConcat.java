package com.android.internal.telephony;

import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class HwPlmnActConcat {
    private static final String TAG = "HwPlmnActConcat";
    private static boolean mNeedConcat = HuaweiTelephonyConfigs.isChinaMobile();

    public static boolean needPlmnActConcat() {
        return mNeedConcat;
    }

    public static String getPlmnActConcat(String plmnValue, ServiceState ss) {
        if (TextUtils.isEmpty(plmnValue)) {
            return null;
        }
        int voiceRegState = ss.getVoiceRegState();
        int dataRegState = ss.getDataRegState();
        int voiceNetworkType = ss.getVoiceNetworkType();
        int dataNetworkType = ss.getDataNetworkType();
        String plmnActValue = plmnValue;
        int networkType = 0;
        if (dataRegState == 0) {
            networkType = dataNetworkType;
        } else if (voiceRegState == 0) {
            networkType = voiceNetworkType;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("plmnValue:");
        stringBuilder.append(plmnValue);
        stringBuilder.append(",voiceNetworkType:");
        stringBuilder.append(voiceNetworkType);
        stringBuilder.append(",dataNetworkType:");
        stringBuilder.append(dataNetworkType);
        stringBuilder.append(",NetworkType:");
        stringBuilder.append(networkType);
        Rlog.d(str, stringBuilder.toString());
        str = null;
        switch (TelephonyManager.getNetworkClass(networkType)) {
            case 1:
                str = "";
                break;
            case 2:
                str = "3G";
                break;
            case 3:
                str = "4G";
                break;
            default:
                Rlog.d(TAG, "network class unknow");
                break;
        }
        if (!(str == null || plmnValue.endsWith(str))) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(plmnActValue);
            stringBuilder2.append(str);
            plmnActValue = stringBuilder2.toString();
        }
        String str2 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("plmnActValue:");
        stringBuilder3.append(plmnActValue);
        Rlog.d(str2, stringBuilder3.toString());
        return plmnActValue;
    }
}
