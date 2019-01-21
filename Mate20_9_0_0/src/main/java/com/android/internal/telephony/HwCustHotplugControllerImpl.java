package com.android.internal.telephony;

import android.content.Context;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class HwCustHotplugControllerImpl extends HwCustHotplugController {
    private static final String CONFIG_SHOW_4_5G_FOR_MCC = "hw_show_4_5G_for_mcc";
    private static final String CONFIG_SHOW_LTE_FOR_MCC = "hw_show_lte_for_mcc";
    private static final String TAG = "HwHotplugController";

    public HwCustHotplugControllerImpl(Context context) {
        super(context);
    }

    private boolean isNeedChange4GString(String configStr) {
        boolean result = false;
        int i = 0;
        if (this.mContext == null) {
            return false;
        }
        String configString = System.getString(this.mContext.getContentResolver(), configStr);
        if (TextUtils.isEmpty(configString)) {
            return false;
        }
        if ("ALL".equalsIgnoreCase(configString)) {
            return true;
        }
        int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        if (default4GSlotId < 0) {
            return false;
        }
        String mccmnc = TelephonyManager.from(this.mContext).getSimOperatorNumericForPhone(default4GSlotId);
        if (!TextUtils.isEmpty(mccmnc)) {
            String[] custValues = configString.trim().split(";");
            while (i < custValues.length) {
                if (mccmnc.startsWith(custValues[i]) || mccmnc.equalsIgnoreCase(custValues[i])) {
                    result = true;
                    break;
                }
                i++;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNeedChange4GString, mccmnc = ");
        stringBuilder.append(mccmnc);
        stringBuilder.append(", configString = ");
        stringBuilder.append(configString);
        Rlog.d(str, stringBuilder.toString());
        return result;
    }

    public String change4GString(String str) {
        String result = str;
        if (this.mContext == null) {
            return result;
        }
        if (isNeedChange4GString(CONFIG_SHOW_LTE_FOR_MCC)) {
            result = result.replace("4G", "LTE");
        }
        if (isNeedChange4GString(CONFIG_SHOW_4_5G_FOR_MCC)) {
            result = result.replace("4G", "4.5G");
        }
        return result;
    }
}
